package com.guitarchords.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guitarchords.app.R
import com.guitarchords.app.ui.icons.AccordioIcons
import com.guitarchords.app.update.UpdateController
import com.guitarchords.app.ui.responsive.WidthClass
import com.guitarchords.app.ui.responsive.rememberWidthClass
import com.guitarchords.app.ui.theme.accordioTopBarColors

private data class HomeAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenTraining: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenTuner: () -> Unit,
    onOpenMetronome: () -> Unit,
    onOpenFinder: () -> Unit,
    onOpenDictionary: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenUpdate: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val update by UpdateController.available.collectAsStateWithLifecycle()
    val actions = listOf(
        HomeAction(stringResource(R.string.home_training), AccordioIcons.revision(), onOpenTraining),
        HomeAction(stringResource(R.string.home_playlists), AccordioIcons.partitura(), onOpenPlaylists),
        HomeAction(stringResource(R.string.home_favorites), AccordioIcons.estrella(), onOpenFavorites),
        HomeAction(stringResource(R.string.home_tuner), Icons.Default.GraphicEq, onOpenTuner),
        HomeAction(stringResource(R.string.metronome_title), AccordioIcons.metronomo(), onOpenMetronome),
        HomeAction(stringResource(R.string.home_finder), AccordioIcons.guitarra(), onOpenFinder),
        HomeAction(stringResource(R.string.home_dictionary), AccordioIcons.acorde(), onOpenDictionary),
        HomeAction(stringResource(R.string.home_trash), Icons.Default.Delete, onOpenTrash),
        HomeAction(stringResource(R.string.home_settings), AccordioIcons.ajustes(), onOpenSettings)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                colors = accordioTopBarColors(),
                title = {
                    // Misma cabecera que la web: marca del mástil y el nombre.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            AccordioIcons.marca(),
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.Unspecified,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(
                            "Accordio",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            )
        }
    ) { pv ->
        val cols = when (rememberWidthClass()) {
            WidthClass.COMPACT -> 2
            WidthClass.MEDIUM -> 3
            WidthClass.EXPANDED -> 3
        }
        Column(modifier = Modifier.fillMaxSize().padding(pv)) {
            // Aviso discreto: solo aparece si la comprobación diaria encontró
            // una versión nueva. Descartarlo no vuelve a molestar en la sesión.
            update?.let { info ->
                UpdateBanner(
                    versionName = info.versionName,
                    onOpen = { UpdateController.dismiss(); onOpenUpdate() },
                    onDismiss = { UpdateController.dismiss() }
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(actions) { a -> HomeTile(a) }
            }
        }
    }
}

@Composable
private fun UpdateBanner(
    versionName: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onOpen() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Icon(
                Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.size(12.dp))
            Text(
                stringResource(R.string.update_available, versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun HomeTile(action: HomeAction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { action.onClick() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    action.icon,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    action.label,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
