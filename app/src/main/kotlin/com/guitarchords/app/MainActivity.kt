package com.guitarchords.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.navigation.compose.rememberNavController
import com.guitarchords.app.data.ZipManager
import com.guitarchords.app.ui.nav.NavGraph
import com.guitarchords.app.ui.nav.Route
import com.guitarchords.app.ui.theme.GuitarChordsTheme
import com.guitarchords.app.ui.theme.ThemeController
import com.guitarchords.app.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val pendingImport = MutableStateFlow<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            val themeMode by ThemeController.mode.collectAsState()
            val dark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            // Iconos de las barras de sistema según el tema elegido (no el del
            // sistema): claros sobre fondo oscuro y viceversa, siempre legibles.
            LaunchedEffect(dark) {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            // dynamic=false: usamos la paleta propia (acordes/texto con contraste
            // garantizado en claro y oscuro), no los colores del fondo de pantalla.
            GuitarChordsTheme(dark = dark, dynamic = false) {
                val nav = rememberNavController()
                val scope = rememberCoroutineScope()
                val pending by pendingImport.collectAsState()
                val app = applicationContext as GuitarChordsApp

                LaunchedEffect(pending) {
                    val uri = pending ?: return@LaunchedEffect
                    scope.launch {
                        val newId = withContext(Dispatchers.IO) {
                            val exp = ZipManager.importFromUri(this@MainActivity, uri)
                            exp?.let { app.repo.importPlaylist(it) }
                        }
                        pendingImport.value = null
                        if (newId != null) {
                            nav.navigate(Route.playlistDetail(newId))
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    // On large screens (tablets, landscape) cap the content
                    // width so lines/cards stay readable instead of stretching.
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .widthIn(max = 840.dp)
                        ) {
                            NavGraph(nav)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            // IntentCompat evita el getParcelableExtra deprecado en API 33+.
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(
                intent, Intent.EXTRA_STREAM, Uri::class.java
            )
            else -> null
        }
        if (uri != null) pendingImport.value = uri
    }
}
