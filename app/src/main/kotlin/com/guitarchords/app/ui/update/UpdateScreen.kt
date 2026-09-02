package com.guitarchords.app.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarchords.app.R
import com.guitarchords.app.ui.theme.accordioTopBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    onBack: () -> Unit,
    vm: UpdateViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                colors = accordioTopBarColors(),
                title = { Text(stringResource(R.string.update_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { pv ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pv)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.SystemUpdate,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                stringResource(R.string.update_current_version, vm.currentVersion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when (val s = state) {
                is UpdateUiState.Checking, is UpdateUiState.Downloading -> {
                    if (s is UpdateUiState.Downloading) {
                        LinearProgressIndicator(
                            progress = { s.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            stringResource(R.string.update_downloading, (s.progress * 100).toInt()),
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.update_checking))
                    }
                }
                is UpdateUiState.UpToDate -> {
                    Icon(
                        Icons.Default.CheckCircle, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(stringResource(R.string.update_up_to_date))
                    CheckButton(vm)
                }
                is UpdateUiState.Available -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.update_available, s.info.versionName),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (s.info.notes.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(s.info.notes, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Button(onClick = { vm.download(s.info) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.update_download_install))
                    }
                }
                is UpdateUiState.NeedsPermission -> {
                    Text(stringResource(R.string.update_needs_permission))
                    Button(onClick = { vm.openPermissionSettings() }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.update_open_settings))
                    }
                    Button(onClick = { vm.installNow(s.file) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.update_install_now))
                    }
                }
                is UpdateUiState.Launched -> {
                    Text(stringResource(R.string.update_installing))
                }
                is UpdateUiState.Error -> {
                    Text(
                        stringResource(R.string.update_error, s.message),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    CheckButton(vm)
                }
                is UpdateUiState.Idle -> {
                    Text(stringResource(R.string.update_intro), style = MaterialTheme.typography.bodyMedium)
                    CheckButton(vm)
                }
            }
        }
    }
}

@Composable
private fun CheckButton(vm: UpdateViewModel) {
    Button(onClick = { vm.check() }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.update_check))
    }
}
