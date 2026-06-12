package com.guitarchords.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guitarchords.app.R
import com.guitarchords.app.ui.responsive.WidthClass
import com.guitarchords.app.ui.responsive.rememberWidthClass

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
    onOpenSettings: () -> Unit
) {
    val actions = listOf(
        HomeAction(stringResource(R.string.home_training), Icons.Default.School, onOpenTraining),
        HomeAction(stringResource(R.string.home_playlists), Icons.Default.LibraryMusic, onOpenPlaylists),
        HomeAction(stringResource(R.string.home_favorites), Icons.Default.Star, onOpenFavorites),
        HomeAction(stringResource(R.string.home_tuner), Icons.Default.GraphicEq, onOpenTuner),
        HomeAction(stringResource(R.string.metronome_title), Icons.Default.Timer, onOpenMetronome),
        HomeAction(stringResource(R.string.home_finder), Icons.Default.Piano, onOpenFinder),
        HomeAction(stringResource(R.string.home_dictionary), Icons.Default.MenuBook, onOpenDictionary),
        HomeAction(stringResource(R.string.home_settings), Icons.Default.Settings, onOpenSettings)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Vivace",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            )
        }
    ) { pv ->
        val cols = when (rememberWidthClass()) {
            WidthClass.COMPACT -> 2
            WidthClass.MEDIUM -> 3
            WidthClass.EXPANDED -> 3
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(cols),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(pv)
        ) {
            items(actions) { a -> HomeTile(a) }
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
