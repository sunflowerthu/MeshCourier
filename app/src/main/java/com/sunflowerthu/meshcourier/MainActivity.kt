package com.sunflowerthu.meshcourier

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sunflowerthu.meshcourier.data.crypto.CspInitializer
import com.sunflowerthu.meshcourier.data.mesh.BleMeshService
import com.sunflowerthu.meshcourier.domain.crypto.CryptoManager
import com.sunflowerthu.meshcourier.presentation.navigation.AppNavHost
import com.sunflowerthu.meshcourier.presentation.navigation.BottomBar
import com.sunflowerthu.meshcourier.presentation.navigation.Screen
import com.sunflowerthu.meshcourier.presentation.theme.MeshCourierTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class ErrorState(
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var cryptoManager: CryptoManager

    private val _chatTarget = MutableStateFlow<String?>(null)
    private val _errorState = MutableStateFlow<ErrorState?>(null)

    private val enableBtLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val btAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (btAdapter.isEnabled) {
            startMeshService()
        } else {
            _errorState.value = ErrorState(
                message = getString(R.string.error_bluetooth_disabled),
                actionLabel = getString(R.string.error_enable),
                onAction = {
                    _errorState.value = null
                    @Suppress("DEPRECATION")
                    enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                }
            )
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val criticalDenied = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            results[Manifest.permission.BLUETOOTH_CONNECT] == false ||
            results[Manifest.permission.BLUETOOTH_SCAN] == false
        } else {
            results[Manifest.permission.ACCESS_FINE_LOCATION] == false
        }

        if (criticalDenied) {
            _errorState.value = ErrorState(
                message = getString(R.string.error_permissions_required),
                actionLabel = getString(R.string.error_open_settings),
                onAction = {
                    _errorState.value = null
                    startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                    )
                }
            )
        } else {
            generateKeysIfNeeded()
            requestEnableBluetoothOrStart()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        intent?.getStringExtra(EXTRA_CONTACT_NODE_ID)?.let { _chatTarget.value = it }

        requestBlePermissionsAndStartService()

        setContent {
            MeshCourierTheme {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                val showBottomBar = currentRoute != Screen.Chat.route

                val errorState by _errorState.collectAsState()
                val bleError by BleMeshService.bleError.collectAsState()

                LaunchedEffect(Unit) {
                    _chatTarget.filterNotNull().collect { nodeId ->
                        navController.navigate(Screen.Chat.createRoute(nodeId))
                        _chatTarget.value = null
                    }
                }

                errorState?.let { err ->
                    AlertDialog(
                        onDismissRequest = { _errorState.value = null },
                        title = { Text(stringResource(R.string.error_action_required)) },
                        text = { Text(err.message) },
                        confirmButton = {
                            if (err.actionLabel != null && err.onAction != null) {
                                TextButton(onClick = err.onAction) { Text(err.actionLabel) }
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { _errorState.value = null }) {
                                Text(stringResource(R.string.common_close))
                            }
                        }
                    )
                }

                // Ошибки BLE из сервиса
                bleError?.let { msg ->
                    AlertDialog(
                        onDismissRequest = { BleMeshService.bleError.value = null },
                        title = { Text(stringResource(R.string.error_bluetooth_title)) },
                        text = { Text(msg) },
                        confirmButton = {
                            TextButton(onClick = {
                                BleMeshService.bleError.value = null
                                startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                            }) { Text(stringResource(R.string.error_bluetooth_settings)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { BleMeshService.bleError.value = null }) {
                                Text(stringResource(R.string.common_close))
                            }
                        }
                    )
                }

                Scaffold(
                    bottomBar = { if (showBottomBar) BottomBar(navController) }
                ) { innerPadding ->
                    AppNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_CONTACT_NODE_ID)?.let { _chatTarget.value = it }
    }

    override fun onResume() {
        super.onResume()
        val btAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (btAdapter.isEnabled && hasBlePermissions()) {
            startMeshService()
        }
    }

    private fun hasBlePermissions(): Boolean {
        val required = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return required.all {
            checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestBlePermissionsAndStartService() {
        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
            } else {
                add(Manifest.permission.BLUETOOTH)
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

        val allGranted = permissions.all {
            checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            generateKeysIfNeeded()
            requestEnableBluetoothOrStart()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    private fun requestEnableBluetoothOrStart() {
        val btAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (!btAdapter.isEnabled) {
            @Suppress("DEPRECATION")
            enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } else {
            startMeshService()
        }
    }

    private fun startMeshService() {
        startForegroundService(Intent(this, BleMeshService::class.java))
    }

    private fun generateKeysIfNeeded() {
        if (cryptoManager.hasOwnKeyPair()) return
        lifecycleScope.launch(Dispatchers.Default) {
            // Wait for CSP to finish initializing (started async in Application.onCreate)
            repeat(20) {
                if (CspInitializer.initialized) return@repeat
                delay(250)
            }
            if (!CspInitializer.initialized) {
                Log.e(TAG, "CSP not ready, skipping auto key generation")
                return@launch
            }
            runCatching { cryptoManager.generateOwnKeyPair() }
                .onFailure { Log.e(TAG, "Auto key generation failed", it) }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        const val EXTRA_CONTACT_NODE_ID = "contact_node_id"
    }
}