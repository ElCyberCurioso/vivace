package com.guitarchords.app.ui.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val lastSync by vm.lastSync.collectAsStateWithLifecycle()
    val pending by vm.pendingUploads.collectAsStateWithLifecycle()

    val running = state is SyncUiState.Running

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sincronización R2") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Atrás") }
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
                "Conecta con el contenedor R2 de Cloudflare a través del Worker. " +
                    "Solo se descarga y sube el contenido nuevo o modificado.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL del Worker") },
                placeholder = { Text("https://guitarchords-sync.tu-cuenta.workers.dev") },
                singleLine = true,
                enabled = !running,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Token de acceso") },
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
                    Text("Sincronizando…")
                } else {
                    Icon(Icons.Default.CloudSync, null)
                    Spacer(Modifier.size(8.dp))
                    Text("Sincronizar ahora")
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "Última sincronización: " +
                            if (lastSync == 0L) "nunca" else formatTime(lastSync),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Canciones con cambios sin subir: $pending",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            when (val s = state) {
                is SyncUiState.Success -> {
                    val r = s.result
                    Text(
                        "Descargadas: ${r.downloaded}  ·  Subidas: ${r.uploaded}" +
                            if (r.conflicts.isNotEmpty()) "  ·  Conflictos: ${r.conflicts.size}" else "",
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

    if (conflicts.isNotEmpty() && !running) {
        ConflictDialog(
            conflicts = conflicts,
            onKeepLocal = { vm.resolveAll(keepLocal = true) },
            onKeepRemote = { vm.resolveAll(keepLocal = false) },
            onDismiss = { vm.dismissConflicts() }
        )
    }
}

@Composable
private fun ConflictDialog(
    conflicts: List<SyncConflict>,
    onKeepLocal: () -> Unit,
    onKeepRemote: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${conflicts.size} conflicto(s)") },
        text = {
            Column {
                Text(
                    "Estas canciones se han modificado tanto en el dispositivo " +
                        "como en el servidor. Elige qué versión conservar para todas:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                conflicts.forEach { c ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            c.title,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "local " + shortTime(c.localUpdatedAt) +
                                " · servidor " + shortTime(c.remoteUpdatedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onKeepLocal) { Text("Mantener local") }
        },
        dismissButton = {
            TextButton(onClick = onKeepRemote) { Text("Usar servidor") }
        }
    )
}

private fun formatTime(ms: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ms))

private fun shortTime(ms: Long): String =
    if (ms == 0L) "—" else SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(ms))
