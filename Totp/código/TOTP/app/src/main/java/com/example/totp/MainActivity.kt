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
import com.example.totp.ui.theme.HomeScreen
import com.example.totp.ui.theme.LoginScreen
import com.example.totp.ui.theme.PasswordManagerScreen
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

/**
 * Pantallas posibles de la aplicación.
 * Se usa un enum en lugar de Navigation Compose para mantener
 * la coherencia con la navegación existente del módulo TOTP.
 */
enum class Screen {
    LOGIN,
    HOME,
    TOTP,
    PASSWORD_MANAGER
}

@Composable
fun AppNavigation(onThemeChanged: () -> Unit) {
    val context = LocalContext.current
    val authStorage = remember { AuthStorage(context) }

    var currentScreen by remember { mutableStateOf(Screen.LOGIN) }
    var showTutorialOnLogin by remember { mutableStateOf(false) }

    when (currentScreen) {
        Screen.LOGIN -> {
            LoginScreen(
                onLoginSuccess = {
                    if (!authStorage.hasSeenTutorial()) {
                        showTutorialOnLogin = true
                        authStorage.setTutorialSeen(true)
                    }
                    currentScreen = Screen.HOME
                }
            )
        }

        Screen.HOME -> {
            HomeScreen(
                onNavigateToTotp = {
                    currentScreen = Screen.TOTP
                },
                onNavigateToPasswordManager = {
                    currentScreen = Screen.PASSWORD_MANAGER
                }
            )
        }

        Screen.TOTP -> {
            TotpMainScreen(
                onThemeChanged = onThemeChanged,
                showTutorialOnStart = showTutorialOnLogin,
                onTutorialShown = { showTutorialOnLogin = false },
                onNavigateHome = {
                    currentScreen = Screen.HOME
                }
            )
        }

        Screen.PASSWORD_MANAGER -> {
            PasswordManagerScreen(
                onBack = {
                    currentScreen = Screen.HOME
                },
                onThemeChanged = onThemeChanged
            )
        }
    }
}