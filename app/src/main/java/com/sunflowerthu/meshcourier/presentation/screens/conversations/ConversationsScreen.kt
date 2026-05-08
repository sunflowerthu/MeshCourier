package com.sunflowerthu.meshcourier.presentation.screens.conversations

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.integration.android.IntentIntegrator
import com.sunflowerthu.meshcourier.R
import com.sunflowerthu.meshcourier.domain.models.MessageStatus
import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.presentation.util.findActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    onOpenChat: (String) -> Unit,
    viewModel: ConversationsViewModel = hiltViewModel()
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    var showNewChatDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val invalidQrText = stringResource(R.string.conversations_invalid_qr)

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text(stringResource(R.string.conversations_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewChatDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.conversations_new_chat_cd))
            }
        }
    ) { padding ->
        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.conversations_empty), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(conversations, key = { it.contactNodeId.value }) { item ->
                    ConversationRow(
                        item = item,
                        onClick = { onOpenChat(item.contactNodeId.value) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showNewChatDialog) {
        NewChatDialog(
            contacts = contacts,
            onConfirm = { nodeId ->
                showNewChatDialog = false
                if (nodeId.isNotBlank()) onOpenChat(nodeId.trim())
            },
            onQrScanned = { rawContent ->
                val nodeId = viewModel.parseAndImportQr(rawContent)
                if (nodeId != null) {
                    nodeId
                } else {
                    scope.launch { snackbarHostState.showSnackbar(invalidQrText) }
                    null
                }
            },
            onDismiss = { showNewChatDialog = false }
        )
    }
}

@Composable
private fun ConversationRow(item: ConversationItem, onClick: () -> Unit) {
    val timeStr = remember(item.lastMessage.createdAt) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.lastMessage.createdAt))
    }
    val processingText = stringResource(R.string.conversations_processing)
    val imageDefault = stringResource(R.string.conversations_attachment_image_default)
    val fileDefault = stringResource(R.string.conversations_attachment_file_default)

    val statusIcon: ImageVector? = if (item.isLastMessageOutgoing) when (item.lastMessage.status) {
        MessageStatus.PENDING     -> Icons.Outlined.Schedule
        MessageStatus.IN_TRANSIT  -> Icons.Filled.Check
        MessageStatus.DELIVERED   -> Icons.Filled.DoneAll
        MessageStatus.TTL_EXPIRED -> Icons.Filled.ErrorOutline
        MessageStatus.PROCESSING  -> null
    } else null
    val isTtlExpired = item.lastMessage.status == MessageStatus.TTL_EXPIRED

    val mime = item.lastMessage.mimeType
    val attachmentIcon: ImageVector? = when {
        mime?.startsWith("image/") == true -> Icons.Filled.Image
        mime != null -> Icons.Filled.AttachFile
        else -> null
    }
    val previewText = remember(item.lastMessage.payload, item.lastMessage.status, item.lastMessage.mimeType, item.lastMessage.fileName) {
        when {
            item.lastMessage.status == MessageStatus.PROCESSING -> processingText
            mime?.startsWith("image/") == true -> item.lastMessage.fileName ?: imageDefault
            mime != null -> item.lastMessage.fileName ?: fileDefault
            else -> runCatching { String(item.lastMessage.payload, Charsets.UTF_8) }
                .getOrDefault("…")
                .take(60)
        }
    }
    val hasUnread = item.unreadCount > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.contactName ?: (item.contactNodeId.value.take(8) + "…"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (statusIcon != null) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isTtlExpired)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (attachmentIcon != null) {
                    Icon(
                        imageVector = attachmentIcon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hasUnread) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (item.unreadCount > 99) "99+" else item.unreadCount.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 10.sp,
                        lineHeight = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun NewChatDialog(
    contacts: Map<NodeId, String>,
    onConfirm: (String) -> Unit,
    onQrScanned: (String) -> String?,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val context = LocalContext.current

    val qrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanned = IntentIntegrator.parseActivityResult(
                result.resultCode, result.data
            )?.contents
            if (!scanned.isNullOrBlank()) {
                val nodeId = onQrScanned(scanned)
                if (nodeId != null) input = nodeId
            }
        }
    }

    val qrPromptText = stringResource(R.string.new_chat_qr_prompt)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_chat_dialog_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(stringResource(R.string.new_chat_node_id_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val activity = context.findActivity() ?: return@OutlinedButton
                        val integrator = IntentIntegrator(activity).apply {
                            setPrompt(qrPromptText)
                            setBeepEnabled(false)
                            setOrientationLocked(false)
                        }
                        qrLauncher.launch(integrator.createScanIntent())
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.new_chat_scan_qr))
                }
                if (contacts.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.new_chat_contacts_section),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Column(modifier = Modifier.heightIn(max = 200.dp)) {
                        contacts.entries.forEachIndexed { index, (nodeId, name) ->
                            if (index > 0) HorizontalDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onConfirm(nodeId.value) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = nodeId.value.take(8) + "…",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(input) }) { Text(stringResource(R.string.common_open)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
