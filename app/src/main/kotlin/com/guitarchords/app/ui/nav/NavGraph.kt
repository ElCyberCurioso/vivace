package com.guitarchords.app.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.guitarchords.app.ui.dictionary.ChordDictionaryScreen
import com.guitarchords.app.ui.favorites.FavoritesScreen
import com.guitarchords.app.ui.finder.ChordFinderScreen
import com.guitarchords.app.ui.home.HomeScreen
import com.guitarchords.app.ui.metronome.MetronomeScreen
import com.guitarchords.app.ui.playlist.PlaylistDetailScreen
import com.guitarchords.app.ui.playlists.PlaylistsScreen
import com.guitarchords.app.ui.search.SongSearchScreen
import com.guitarchords.app.ui.settings.SettingsScreen
import com.guitarchords.app.ui.song.SongEditorScreen
import com.guitarchords.app.ui.song.SongViewScreen
import com.guitarchords.app.ui.song.VersionEditorScreen
import com.guitarchords.app.ui.sync.SyncScreen
import com.guitarchords.app.ui.training.AreaPathScreen
import com.guitarchords.app.ui.training.ExerciseRunnerScreen
import com.guitarchords.app.ui.training.PlacementScreen
import com.guitarchords.app.ui.training.StatsScreen
import com.guitarchords.app.ui.training.TrainingDashboardScreen
import com.guitarchords.app.ui.tuner.TunerScreen
import com.guitarchords.app.ui.unassigned.UnassignedSongsScreen
import com.guitarchords.app.training.TrainingArea

object Route {
    const val Home = "home"
    const val Settings = "settings"
    const val Playlists = "playlists"
    const val PlaylistDetail = "playlist/{id}"
    const val SongView = "song/view/{id}"
    const val SongEdit = "song/edit/{id}"
    const val SongNew = "song/new/{playlistId}"
    const val VersionEdit = "version/edit/{id}"
    const val ChordDictionary = "chords/dictionary"
    const val ChordFinder = "chords/finder"
    const val Tuner = "tuner"
    const val Metronome = "metronome"
    const val SongSearch = "songs/search"
    const val Favorites = "songs/favorites"
    const val Unassigned = "songs/unassigned"
    const val Sync = "sync"
    const val Training = "training"
    const val TrainingPlacement = "training/placement"
    const val TrainingAreaPath = "training/area/{area}"
    const val TrainingExercise = "training/exercise/{id}"
    const val TrainingStats = "training/stats"

    fun trainingArea(area: TrainingArea) = "training/area/${area.name}"
    fun trainingExercise(id: String) = "training/exercise/$id"

    fun playlistDetail(id: Long) = "playlist/$id"
    fun songView(id: Long) = "song/view/$id"
    fun songEdit(id: Long) = "song/edit/$id"
    fun songNew(playlistId: Long) = "song/new/$playlistId"
    fun versionEdit(id: Long) = "version/edit/$id"
}

@Composable
fun NavGraph(nav: NavHostController) {
    NavHost(
        nav,
        startDestination = Route.Home,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(260)
            ) + fadeIn(tween(260))
        },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(260)
            ) + fadeOut(tween(260))
        }
    ) {
        composable(Route.Home) {
            HomeScreen(
                onOpenTraining = { nav.navigate(Route.Training) },
                onOpenPlaylists = { nav.navigate(Route.Playlists) },
                onOpenFavorites = { nav.navigate(Route.Favorites) },
                onOpenTuner = { nav.navigate(Route.Tuner) },
                onOpenMetronome = { nav.navigate(Route.Metronome) },
                onOpenFinder = { nav.navigate(Route.ChordFinder) },
                onOpenDictionary = { nav.navigate(Route.ChordDictionary) },
                onOpenSettings = { nav.navigate(Route.Settings) }
            )
        }
        composable(Route.Settings) {
            SettingsScreen(
                onOpenSync = { nav.navigate(Route.Sync) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Route.Playlists) {
            PlaylistsScreen(
                onPlaylistClick = { nav.navigate(Route.playlistDetail(it)) },
                onOpenSearch = { nav.navigate(Route.SongSearch) },
                onAddSong = { nav.navigate(Route.songNew(0L)) },
                onOpenUnassigned = { nav.navigate(Route.Unassigned) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Route.ChordDictionary) {
            ChordDictionaryScreen(onBack = { nav.popBackStack() })
        }
        composable(Route.ChordFinder) {
            ChordFinderScreen(onBack = { nav.popBackStack() })
        }
        composable(Route.Tuner) {
            TunerScreen(onBack = { nav.popBackStack() })
        }
        composable(Route.Metronome) {
            MetronomeScreen(onBack = { nav.popBackStack() })
        }
        composable(Route.SongSearch) {
            SongSearchScreen(
                onSongClick = { nav.navigate(Route.songView(it)) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Route.Favorites) {
            FavoritesScreen(
                onSongClick = { nav.navigate(Route.songView(it)) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Route.Unassigned) {
            UnassignedSongsScreen(
                onSongClick = { nav.navigate(Route.songView(it)) },
                onEditSong = { nav.navigate(Route.songEdit(it)) },
                onAddSong = { nav.navigate(Route.songNew(0L)) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Route.Sync) {
            SyncScreen(onBack = { nav.popBackStack() })
        }
        composable(Route.Training) {
            TrainingDashboardScreen(
                onOpenExercise = { nav.navigate(Route.trainingExercise(it)) },
                onOpenArea = { nav.navigate(Route.trainingArea(it)) },
                onOpenPlacement = { nav.navigate(Route.TrainingPlacement) },
                onOpenStats = { nav.navigate(Route.TrainingStats) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(
            Route.TrainingAreaPath,
            arguments = listOf(navArgument("area") { type = NavType.StringType })
        ) { entry ->
            val name = entry.arguments?.getString("area") ?: return@composable
            val area = TrainingArea.entries.firstOrNull { it.name == name } ?: return@composable
            AreaPathScreen(
                area = area,
                onOpenExercise = { nav.navigate(Route.trainingExercise(it)) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Route.TrainingPlacement) {
            PlacementScreen(
                onDone = { nav.popBackStack() },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Route.TrainingStats) {
            StatsScreen(
                onRecalibrate = { nav.navigate(Route.TrainingPlacement) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(
            Route.TrainingExercise,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            ExerciseRunnerScreen(
                exerciseId = id,
                onBack = { nav.popBackStack() }
            )
        }
        composable(
            Route.PlaylistDetail,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            PlaylistDetailScreen(
                playlistId = id,
                onSongClick = { nav.navigate(Route.songView(it)) },
                onAddSong = { nav.navigate(Route.songNew(id)) },
                onEditSong = { nav.navigate(Route.songEdit(it)) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(
            Route.SongView,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            SongViewScreen(
                songId = id,
                onEdit = { nav.navigate(Route.songEdit(id)) },
                onEditVersion = { nav.navigate(Route.versionEdit(it)) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(
            Route.VersionEdit,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            VersionEditorScreen(
                versionId = id,
                onDone = { nav.popBackStack() }
            )
        }
        composable(
            Route.SongEdit,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            SongEditorScreen(
                songId = id,
                playlistId = 0L,
                onDone = { nav.popBackStack() }
            )
        }
        composable(
            Route.SongNew,
            arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
        ) { entry ->
            val pid = entry.arguments?.getLong("playlistId") ?: return@composable
            SongEditorScreen(
                songId = 0L,
                playlistId = pid,
                onDone = { nav.popBackStack() }
            )
        }
    }
}
