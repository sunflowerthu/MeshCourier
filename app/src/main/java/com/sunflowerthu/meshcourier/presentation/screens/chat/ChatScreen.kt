package com.sunflowerthu.meshcourier.presentation.screens.chat

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunflowerthu.meshcourier.R
import com.sunflowerthu.meshcourier.domain.models.Message
import com.sunflowerthu.meshcourier.domain.models.MessageStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_MESSAGE_LENGTH = 5000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    contactNodeId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val encryptionAvailable by viewModel.encryptionAvailable.collectAsStateWithLifecycle()
    val contactName by viewModel.contactName.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var inputText by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.sendFile(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    LaunchedEffect(messages.size) {
        viewModel.markRead()
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    if (showRenameDialog) {
        RenameContactDialog(
            currentName = contactName ?: "",
            onConfirm = { name ->
                viewModel.renameContact(name)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.chat_delete_contact_title)) },
            text = { Text(stringResource(R.string.chat_delete_contact_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteContact()
                    showDeleteDialog = false
                }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(contactName ?: (contactNodeId.take(8) + "…"))
                        if (contactName != null) {
                            Text(
                                text = contactNodeId.take(8) + "…",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.chat_back_cd))
                    }
                },
                actions = {
                    IconButton(onClick = { showRenameDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.chat_rename_cd))
                    }
                    IconButton(
                        onClick = { if (!encryptionAvailable) viewModel.sendKeyExchange() },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (encryptionAvailable) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = stringResource(
                                if (encryptionAvailable) R.string.chat_encrypted_cd else R.string.chat_exchange_keys_cd
                            ),
                            tint = if (encryptionAvailable)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.chat_menu_cd))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_menu_delete_contact)) },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages, key = { it.messageId.toString() }) { msg ->
                    MessageBubble(
                        message = msg,
                        isOutgoing = msg.senderNodeId == viewModel.myNodeId,
                        onOpenAttachment = { viewModel.openAttachment(msg) }
                    )
                }
            }

            if (!encryptionAvailable) {
                Text(
                    text = stringResource(R.string.chat_unencrypted_warning),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            MessageInput(
                text = inputText,
                onTextChange = { if (it.length <= MAX_MESSAGE_LENGTH) inputText = it },
                onSend = {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                },
                onAttachFile = { fileLauncher.launch("*/*") }
            )
        }
    }
}

@Composable
private fun MessageBubble(message: Message, isOutgoing: Boolean, onOpenAttachment: () -> Unit) {
    val timeStr = remember(message.createdAt) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.createdAt))
    }
    val isTtlExpired = message.status == MessageStatus.TTL_EXPIRED
    val statusIcon: ImageVector? = when {
        isTtlExpired -> Icons.Filled.ErrorOutline
        isOutgoing -> when (message.status) {
            MessageStatus.PENDING     -> Icons.Outlined.Schedule
            MessageStatus.IN_TRANSIT  -> Icons.Filled.Check
            MessageStatus.DELIVERED   -> Icons.Filled.DoneAll
            MessageStatus.PROCESSING  -> null
            MessageStatus.TTL_EXPIRED -> Icons.Filled.ErrorOutline
        }
        else -> null
    }

    val processingText = if (!isOutgoing && message.status == MessageStatus.PROCESSING) "..." else null

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isOutgoing) 16.dp else 4.dp,
                bottomEnd = if (isOutgoing) 4.dp else 16.dp
            ),
            color = if (isOutgoing)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                when {
                    message.mimeType?.startsWith("image/") == true -> {
                        val bitmap = remember(message.messageId) {
                            runCatching {
                                BitmapFactory.decodeByteArray(message.payload, 0, message.payload.size)
                            }.getOrNull()
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = message.fileName ?: stringResource(R.string.chat_image_default_cd),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .widthIn(max = 240.dp)
                                    .padding(bottom = 4.dp)
                                    .clickable(onClick = onOpenAttachment)
                            )
                        } else {
                            FileAttachmentRow(
                                fileName = message.fileName ?: stringResource(R.string.chat_file_default),
                                onClick = onOpenAttachment
                            )
                        }
                    }
                    message.mimeType != null -> {
                        FileAttachmentRow(
                            fileName = message.fileName ?: stringResource(R.string.chat_file_default),
                            onClick = onOpenAttachment
                        )
                    }
                    else -> {
                        val text = remember(message.payload) {
                            runCatching { String(message.payload, Charsets.UTF_8) }.getOrDefault("…")
                        }
                        Text(text = text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isOutgoing && message.hopCount > 0) {
                        Text(
                            text = stringResource(R.string.chat_hop_count, message.hopCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val statusTint = if (isTtlExpired)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                    if (statusIcon != null) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = statusTint
                        )
                    } else if (processingText != null) {
                        Text(
                            text = processingText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusTint
                        )
                    }
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FileAttachmentRow(fileName: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(bottom = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Icon(
            Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = fileName,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2
        )
    }
}

@Composable
private fun RenameContactDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_rename_dialog_title)) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { if (it.length <= 50) input = it },
                label = { Text(stringResource(R.string.chat_rename_dialog_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(input) }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachFile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = onAttachFile) {
            Icon(
                Icons.Default.AttachFile,
                contentDescription = stringResource(R.string.chat_attach_file_cd),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.chat_message_placeholder)) },
            maxLines = 4,
            shape = RoundedCornerShape(24.dp)
        )
        IconButton(
            onClick = onSend,
            enabled = text.isNotBlank()
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.chat_send_cd),
                tint = if (text.isNotBlank())
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
