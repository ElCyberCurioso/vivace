package com.guitarchords.app.ui.sync

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarchords.app.R
import com.guitarchords.app.sync.PendingUpload
import com.guitarchords.app.sync.SyncConflict
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    onBack: () -> Unit,
    vm: SyncViewModel = viewModel()
) {
    var url by remember { mutableStateOf(vm.initialUrl) }
    var token by remember { mutableStateOf(vm.initialToken) }

    val state by vm.state.collectAsStateWithLifecycle()
    val conflicts by vm.conflicts.collectAsStateWithLifecycle()
    val pendingPush by vm.pendingPush.collectAsStateWithLifecycle()
    val lastSync by vm.lastSync.collectAsStateWithLifecycle()
    val pending by vm.pendingUploads.collectAsStateWithLifecycle()

    val running = state is SyncUiState.Running

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sync_r2_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                }
            )
        }
    ) { pv ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pv)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                stringResource(R.string.sync_intro),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResource(R.string.sync_worker_url)) },
                placeholder = { Text("https://guitarchords-sync.tu-cuenta.workers.dev") },
                singleLine = true,
                enabled = !running,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text(stringResource(R.string.sync_token)) },
                singleLine = true,
                enabled = !running,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { vm.sync(url, token) },
                enabled = !running,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (running) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.syncing))
                } else {
                    Icon(Icons.Default.CloudSync, null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.sync_now))
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        stringResource(
                            R.string.last_sync,
                            if (lastSync == 0L) stringResource(R.string.never) else formatTime(lastSync)
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        stringResource(R.string.pending_uploads, pending),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            when (val s = state) {
                is SyncUiState.Success -> {
                    val r = s.result
                    Text(
                        stringResource(R.string.sync_result, r.downloaded, r.uploaded) +
                            if (r.conflicts.isNotEmpty())
                                stringResource(R.string.sync_conflicts_suffix, r.conflicts.size)
                            else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is SyncUiState.Error -> {
                    Text(
                        s.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {}
            }
        }
    }

    // Como en git: primero se integran los conflictos, después se confirma el push.
    if (conflicts.isNotEmpty() && !running) {
        ConflictDialog(
            conflicts = conflicts,
            onResolve = { c, keepLocal -> vm.resolveOne(c, keepLocal) },
            onDismiss = { vm.dismissConflicts() }
        )
    } else if (pendingPush.isNotEmpty() && !running) {
        PushConfirmDialog(
            pending = pendingPush,
            onConfirm = { vm.confirmPush() },
            onCancel = { vm.cancelPush() }
        )
    }
}

@Composable
private fun PushConfirmDialog(
    pending: List<PendingUpload>,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.push_confirm_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.push_confirm_msg, pending.size),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    pending.forEach { p ->
                        Text(
                            "• " + p.title.ifBlank { stringResource(R.string.untitled) } +
                                if (p.isNew) "  (" + stringResource(R.string.push_new_song) + ")" else "",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.push_upload)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.push_not_now)) }
        }
    )
}

@Composable
private fun ConflictDialog(
    conflicts: List<SyncConflict>,
    onResolve: (SyncConflict, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.conflicts_title, conflicts.size)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.conflicts_msg),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    conflicts.forEach { c ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(c.title, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    stringResource(
                                        R.string.local_vs_server,
                                        shortTime(c.localUpdatedAt),
                                        shortTime(c.remoteUpdatedAt)
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { onResolve(c, true) }) {
                                Text(stringResource(R.string.conflict_local_short))
                            }
                            TextButton(onClick = { onResolve(c, false) }) {
                                Text(stringResource(R.string.conflict_server_short))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.conflict_later)) }
        }
    )
}

private fun formatTime(ms: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ms))

private fun shortTime(ms: Long): String =
    if (ms == 0L) "—" else SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(ms))
