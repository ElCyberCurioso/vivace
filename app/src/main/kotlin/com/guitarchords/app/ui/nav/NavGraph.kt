package com.guitarchords.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.guitarchords.app.ui.dictionary.ChordDictionaryScreen
import com.guitarchords.app.ui.finder.ChordFinderScreen
import com.guitarchords.app.ui.playlist.PlaylistDetailScreen
import com.guitarchords.app.ui.playlists.PlaylistsScreen
import com.guitarchords.app.ui.song.SongEditorScreen
import com.guitarchords.app.ui.song.SongViewScreen
import com.guitarchords.app.ui.tuner.TunerScreen

object Route {
    const val Playlists = "playlists"
    const val PlaylistDetail = "playlist/{id}"
    const val SongView = "song/view/{id}"
    const val SongEdit = "song/edit/{id}"
    const val SongNew = "song/new/{playlistId}"
    const val ChordDictionary = "chords/dictionary"
    const val ChordFinder = "chords/finder"
    const val Tuner = "tuner"

    fun playlistDetail(id: Long) = "playlist/$id"
    fun songView(id: Long) = "song/view/$id"
    fun songEdit(id: Long) = "song/edit/$id"
    fun songNew(playlistId: Long) = "song/new/$playlistId"
}

@Composable
fun NavGraph(nav: NavHostController) {
    NavHost(nav, startDestination = Route.Playlists) {
        composable(Route.Playlists) {
            PlaylistsScreen(
                onPlaylistClick = { nav.navigate(Route.playlistDetail(it)) },
                onOpenDictionary = { nav.navigate(Route.ChordDictionary) },
                onOpenFinder = { nav.navigate(Route.ChordFinder) },
                onOpenTuner = { nav.navigate(Route.Tuner) }
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
                onBack = { nav.popBackStack() }
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
