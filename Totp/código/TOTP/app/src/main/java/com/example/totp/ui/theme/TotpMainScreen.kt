package com.example.totp.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.totp.model.TotpAccount
import com.example.totp.model.TotpDatabase
import com.example.totp.totp.SecureStorage
import com.example.totp.totp.TotpGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TotpMainScreen() {
    val context = LocalContext.current
    val database = remember { TotpDatabase.getDatabase(context) }
    val dao = remember { database.totpAccountDao() }
    val secureStorage = remember { SecureStorage(context) }
    val coroutineScope = rememberCoroutineScope()

    // Obtener las cuentas de Room (Flow → State)
    val accounts by dao.getAllAccounts().collectAsState(initial = emptyList())

    // Estado del temporizador
    var secondsRemaining by remember { mutableIntStateOf(30) }
    var codes by remember { mutableStateOf(mapOf<Int, String>()) }

    // Estado del diálogo para añadir cuenta
    var showAddDialog by remember { mutableStateOf(false) }

    // Estado de la pantalla del escáner QR
    var showQrScanner by remember { mutableStateOf(false) }

    // Temporizador
    LaunchedEffect(Unit) {
        while (true) {
            val generator = TotpGenerator()
            secondsRemaining = generator.secondsRemaining()

            val currentAccounts = accounts
            val newCodes = mutableMapOf<Int, String>()
            for (account in currentAccounts) {
                try {
                    val secretKey = secureStorage.getSecretKey(account.id)
                    if (secretKey != null) {
                        val gen = TotpGenerator(
                            period = account.period,
                            digits = account.digits,
                            algorithm = account.algorithm
                        )
                        newCodes[account.id] = gen.generateCode(secretKey)
                    } else {
                        newCodes[account.id] = "NO KEY"
                    }
                } catch (e: Exception) {
                    newCodes[account.id] = "ERROR"
                }
            }
            codes = newCodes

            delay(1000L)
        }
    }

    // Diálogo para añadir cuenta manualmente
    if (showAddDialog) {
        AddAccountDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, issuer, secretKey ->
                coroutineScope.launch {
                    val account = TotpAccount(
                        name = name,
                        issuer = issuer
                    )
                    dao.insertAccount(account)

                    val allAccounts = dao.getAllAccountsOnce()
                    val savedAccount = allAccounts.lastOrNull()

                    if (savedAccount != null) {
                        secureStorage.saveSecretKey(savedAccount.id, secretKey)
                    }
                }
                showAddDialog = false
            }
        )
    }

    // Pantalla del escáner QR
    if (showQrScanner) {
        QrScannerScreen(
            onQrScanned = { otpData ->
                // Validar que la SecretKey es Base32 válida antes de crear la cuenta
                try {
                    val decoded = com.example.totp.totp.Base32.decode(otpData.secretKey)
                    if (decoded.isEmpty() || otpData.secretKey.isBlank()) {
                        // No crear la cuenta, volver con error
                        return@QrScannerScreen "QR inválido: la SecretKey está vacía o no es válida"
                    }
                    // Verificar que genera un código TOTP correctamente
                    val generator = com.example.totp.totp.TotpGenerator()
                    generator.generateCode(otpData.secretKey)

                    // Si todo es válido, guardar la cuenta
                    coroutineScope.launch {
                        val account = TotpAccount(
                            name = otpData.name,
                            issuer = otpData.issuer,
                            algorithm = otpData.algorithm,
                            digits = otpData.digits,
                            period = otpData.period
                        )
                        dao.insertAccount(account)

                        val allAccounts = dao.getAllAccountsOnce()
                        val savedAccount = allAccounts.lastOrNull()
                        if (savedAccount != null) {
                            secureStorage.saveSecretKey(savedAccount.id, otpData.secretKey)
                        }
                    }
                    showQrScanner = false
                    return@QrScannerScreen null // Sin error
                } catch (e: Exception) {
                    return@QrScannerScreen "QR inválido: la SecretKey no es Base32 válida (solo A-Z y 2-7)"
                }
            },
            onBack = { showQrScanner = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TOTP Authenticator") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            var showMenu by remember { mutableStateOf(false) }

            Box {
                FloatingActionButton(
                    onClick = { showMenu = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir cuenta")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Introducir manualmente") },
                        onClick = {
                            showMenu = false
                            showAddDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Escanear QR") },
                        onClick = {
                            showMenu = false
                            showQrScanner = true
                        }
                    )
                }
            }
        }
    ) { padding ->
        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay cuentas.\nAñade una con el botón +",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(accounts, key = { it.id }) { account ->
                    TotpCard(
                        account = account,
                        code = codes[account.id] ?: "------",
                        secondsRemaining = secondsRemaining,
                        onDelete = {
                            coroutineScope.launch {
                                secureStorage.deleteSecretKey(account.id)
                                dao.deleteAccount(account)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TotpCard(
    account: TotpAccount,
    code: String,
    secondsRemaining: Int,
    onDelete: () -> Unit
) {
    val progress by animateFloatAsState(
        targetValue = secondsRemaining / account.period.toFloat(),
        label = "progress"
    )

    val progressColor by animateColorAsState(
        targetValue = if (secondsRemaining <= 5)
            MaterialTheme.colorScheme.error
        else
            MaterialTheme.colorScheme.primary,
        label = "progressColor"
    )

    val timerColor by animateColorAsState(
        targetValue = if (secondsRemaining <= 5)
            MaterialTheme.colorScheme.error
        else
            MaterialTheme.colorScheme.onSurface,
        label = "timerColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = account.issuer,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar cuenta",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${secondsRemaining}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = timerColor
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = account.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = code.chunked(3).joinToString(" "),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = progressColor
            )
        }
    }
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, issuer: String, secretKey: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var issuer by remember { mutableStateOf("") }
    var secretKey by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir cuenta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = issuer,
                    onValueChange = { issuer = it },
                    label = { Text("Servicio (ej: Google)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Cuenta (ej: usuario@gmail.com)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = secretKey,
                    onValueChange = { secretKey = it.uppercase().replace(" ", "") },
                    label = { Text("SecretKey (Base32)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showError) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank() || issuer.isBlank() || secretKey.isBlank()) {
                        errorMessage = "Todos los campos son obligatorios"
                        showError = true
                    } else {
                        try {
                            val decoded = com.example.totp.totp.Base32.decode(secretKey)
                            if (decoded.isEmpty()) {
                                errorMessage = "La SecretKey no es válida"
                                showError = true
                            } else {
                                val generator = com.example.totp.totp.TotpGenerator()
                                generator.generateCode(secretKey)
                                onAdd(name, issuer, secretKey)
                            }
                        } catch (e: Exception) {
                            errorMessage = "La SecretKey no es Base32 válida (solo A-Z y 2-7)"
                            showError = true
                        }
                    }
                }
            ) {
                Text("Añadir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}