package com.sunflowerthu.meshcourier.presentation.screens.nearby

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunflowerthu.meshcourier.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyNodesScreen(
    onOpenChat: (String) -> Unit,
    viewModel: NearbyNodesViewModel = hiltViewModel()
) {
    val nodeItems by viewModel.nodeItems.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val noKeysText = stringResource(R.string.chat_event_no_own_keys)

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nearby_title)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Статистика сети
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(label = stringResource(R.string.nearby_stat_nodes), value = stats.activeNodes.toString())
                    StatItem(label = stringResource(R.string.nearby_stat_sent), value = stats.packetsSent.toString())
                    StatItem(label = stringResource(R.string.nearby_stat_relayed), value = stats.packetsRelayed.toString())
                }
            }

            if (nodeItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.nearby_empty), style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(nodeItems, key = { it.nodeId.value }) { item ->
                        NearbyNodeRow(
                            item = item,
                            onExchangeKeys = {
                                if (viewModel.hasOwnKeyPair()) {
                                    viewModel.sendKeyExchange(item.nodeId)
                                } else {
                                    Toast.makeText(context, noKeysText, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onOpenChat = { onOpenChat(item.nodeId.value) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NearbyNodeRow(
    item: NearbyNodeItem,
    onExchangeKeys: () -> Unit,
    onOpenChat: () -> Unit,
) {
    val justNow = stringResource(R.string.nearby_last_seen_just_now)
    val minutesFmt = stringResource(R.string.nearby_last_seen_minutes)
    val hoursFmt = stringResource(R.string.nearby_last_seen_hours)
    val lastSeenText = item.lastSeenAt?.let { formatLastSeen(it, justNow, minutesFmt, hoursFmt) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (item.contactName != null) {
                Text(text = item.contactName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = item.nodeId.value.take(8) + "…",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(text = item.nodeId.value.take(8) + "…", style = MaterialTheme.typography.bodyLarge)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    modifier = Modifier.size(7.dp),
                    shape = CircleShape,
                    color = Color(0xFF4CAF50)
                ) {}
                Text(
                    text = stringResource(R.string.nearby_status_available),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4CAF50)
                )
                if (lastSeenText != null) {
                    Text(
                        text = "· $lastSeenText",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (item.hasKey) {
            Button(onClick = onOpenChat) { Text(stringResource(R.string.nearby_open_chat)) }
        } else {
            OutlinedButton(onClick = onExchangeKeys) { Text(stringResource(R.string.nearby_exchange_keys)) }
        }
    }
}

private fun formatLastSeen(
    timestamp: Long,
    justNow: String,
    minutesFmt: String,
    hoursFmt: String,
): String {
    val diffMs = System.currentTimeMillis() - timestamp
    val diffMin = diffMs / 60_000
    return when {
        diffMin < 1   -> justNow
        diffMin < 60  -> minutesFmt.format(diffMin)
        else          -> hoursFmt.format(diffMin / 60)
    }
}
