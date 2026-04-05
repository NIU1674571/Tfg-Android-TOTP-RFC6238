package com.example.totp.ui.theme

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.totp.totp.AuthStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val authStorage = remember { AuthStorage(context) }

    val isRegistered = remember { authStorage.isUserRegistered() }
    var isRegisterMode by remember { mutableStateOf(!isRegistered) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Intentar biometría al iniciar si está activada
    LaunchedEffect(Unit) {
        if (isRegistered && authStorage.isBiometricEnabled()) {
            showBiometricPrompt(context as FragmentActivity, onLoginSuccess)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TOTP Authenticator") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isRegisterMode) "Crear cuenta" else "Iniciar sesión",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Usuario") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            if (isRegisterMode) {
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (showError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isRegisterMode) {
                        // Registro
                        if (username.isBlank() || password.isBlank()) {
                            errorMessage = "Todos los campos son obligatorios"
                            showError = true
                        } else if (password.length < 4) {
                            errorMessage = "La contraseña debe tener al menos 4 caracteres"
                            showError = true
                        } else if (password != confirmPassword) {
                            errorMessage = "Las contraseñas no coinciden"
                            showError = true
                        } else {
                            authStorage.registerUser(username, password)
                            showError = false
                            onLoginSuccess()
                        }
                    } else {
                        // Login
                        if (authStorage.verifyLogin(username, password)) {
                            showError = false
                            onLoginSuccess()
                        } else {
                            errorMessage = "Usuario o contraseña incorrectos"
                            showError = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isRegisterMode) "Registrarse" else "Entrar")
            }

            // Botón de biometría (solo si está registrado y activada)
            if (!isRegisterMode && authStorage.isBiometricEnabled()) {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        showBiometricPrompt(context as FragmentActivity, onLoginSuccess)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometría",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Iniciar con biometría")
                }
            }
        }
    }
}

/**
 * Muestra el diálogo de biometría del sistema.
 */
private fun showBiometricPrompt(activity: FragmentActivity, onSuccess: () -> Unit) {
    val biometricManager = BiometricManager.from(activity)
    val canAuthenticate = biometricManager.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
    )

    if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
        Toast.makeText(activity, "Biometría no disponible en este dispositivo", Toast.LENGTH_SHORT).show()
        return
    }

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("TOTP Authenticator")
        .setSubtitle("Inicia sesión con tu huella o cara")
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()

    val biometricPrompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // No hacer nada, el usuario puede intentar con contraseña
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(activity, "Autenticación fallida", Toast.LENGTH_SHORT).show()
            }
        }
    )

    biometricPrompt.authenticate(promptInfo)
}
