package com.example.totp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.totp.ui.theme.LoginScreen
import com.example.totp.ui.TotpMainScreen
import com.example.totp.ui.theme.TOTPTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TOTPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var isLoggedIn by remember { mutableStateOf(false) }

    if (isLoggedIn) {
        TotpMainScreen()
    } else {
        LoginScreen(
            onLoginSuccess = { isLoggedIn = true }
        )
    }
}
