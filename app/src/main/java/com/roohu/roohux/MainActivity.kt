package com.roohu.roohux

import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roohu.roohux.core.design.LocalReduceMotion
import com.roohu.roohux.core.design.RoohuXTheme
import com.roohu.roohux.core.design.isDark
import com.roohu.roohux.core.navigation.RoohuXNavHost
import com.roohu.roohux.core.settings.AppSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val container = (application as RoohuXApp).container
        // The splash stays only until saved settings (theme etc.) are really loaded, so the first frame never
        // flashes the wrong theme; a 1.5 s cap guarantees it can never hang.
        var ready = false
        splash.setKeepOnScreenCondition { !ready }
        lifecycleScope.launch {
            withTimeoutOrNull(1_500) { container.settings.settings.first() }
            ready = true
        }
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(AppSettings())
            val dark = isDark(settings.theme)
            val systemReduce = remember {
                Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
            }
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose { }
            }
            CompositionLocalProvider(LocalReduceMotion provides (settings.reduceMotion || systemReduce)) {
                RoohuXTheme(dark) { RoohuXNavHost(container) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (application as RoohuXApp).container.player.connect()
    }

    override fun onStop() {
        // Releases the MediaController (no leak). Audio keeps playing in the service; video is paused.
        (application as RoohuXApp).container.player.release()
        super.onStop()
    }
}
