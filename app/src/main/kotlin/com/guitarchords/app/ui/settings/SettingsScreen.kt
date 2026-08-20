package com.guitarchords.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guitarchords.app.R
import com.guitarchords.app.ui.theme.ThemeController
import com.guitarchords.app.ui.theme.ThemeMode

private data class SettingRow(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenSync: () -> Unit,
    onOpenUpdate: () -> Unit,
    onBack: () -> Unit
) {
    val rows = listOf(
        SettingRow(
            stringResource(R.string.sync_r2_title),
            stringResource(R.string.sync_r2_subtitle),
            Icons.Default.CloudSync,
            onOpenSync
        ),
        SettingRow(
            stringResource(R.string.update_title),
            stringResource(R.string.update_subtitle),
            Icons.Default.SystemUpdate,
            onOpenUpdate
        )
    )

    SettingsContent(rows = rows, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeCard(current: ThemeMode, onPick: (ThemeMode) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Brightness6, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(12.dp))
                Text(
                    stringResource(R.string.theme_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(Modifier.size(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val options = listOf(
                    ThemeMode.SYSTEM to R.string.theme_system,
                    ThemeMode.LIGHT to R.string.theme_light,
                    ThemeMode.DARK to R.string.theme_dark
                )
                options.forEach { (mode, label) ->
                    FilterChip(
                        selected = current == mode,
                        onClick = { onPick(mode) },
                        label = { Text(stringResource(label)) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(rows: List<SettingRow>, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                }
            )
        }
    ) { pv ->
        val themeMode by ThemeController.mode.collectAsStateWithLifecycle()
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize().padding(pv)
        ) {
            item {
                ThemeCard(current = themeMode, onPick = { ThemeController.setMode(it) })
            }
            items(rows.size) { i ->
                val r = rows[i]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { r.onClick() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(14.dp)
                    ) {
                        Icon(
                            r.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.size(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                r.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                r.subtitle,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
    }
}
