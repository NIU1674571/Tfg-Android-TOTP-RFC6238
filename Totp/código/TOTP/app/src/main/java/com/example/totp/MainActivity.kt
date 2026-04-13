package com.example.totp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.example.totp.totp.AuthStorage
import com.example.totp.totp.ThemeStorage
import com.example.totp.ui.theme.LoginScreen
import com.example.totp.ui.theme.TotpMainScreen
import com.example.totp.ui.theme.TOTPTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppWithTheme()
        }
    }
}

@Composable
fun AppWithTheme() {
    val context = LocalContext.current
    val themeStorage = remember { ThemeStorage(context) }

    // Estado que fuerza la recomposición cuando cambia el tema
    var themeVersion by remember { mutableIntStateOf(0) }

    // Leer preferencias (se re-lee cuando themeVersion cambia)
    val themeMode = remember(themeVersion) { themeStorage.getThemeMode() }

    val isDarkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    TOTPTheme(darkTheme = isDarkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavigation(
                onThemeChanged = { themeVersion++ }
            )
        }
    }
}

@Composable
fun AppNavigation(onThemeChanged: () -> Unit) {
    val context = LocalContext.current
    val authStorage = remember { AuthStorage(context) }

    var isLoggedIn by remember { mutableStateOf(false) }
    var showTutorialOnLogin by remember { mutableStateOf(false) }

    if (isLoggedIn) {
        TotpMainScreen(
            onThemeChanged = onThemeChanged,
            showTutorialOnStart = showTutorialOnLogin,
            onTutorialShown = { showTutorialOnLogin = false }
        )
    } else {
        LoginScreen(
            onLoginSuccess = {
                isLoggedIn = true
                if (!authStorage.hasSeenTutorial()) {
                    showTutorialOnLogin = true
                    authStorage.setTutorialSeen(true)
                }
            }
        )
    }
}
