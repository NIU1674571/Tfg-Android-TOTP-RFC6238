package com.example.totp.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.totp.model.TotpAccount
import com.example.totp.model.TotpDatabase
import com.example.totp.totp.SecureStorage
import com.example.totp.totp.TotpGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TotpMainScreen() {
    val context = LocalContext.current
    val database = remember { TotpDatabase.getDatabase(context) }
    val dao = remember { database.totpAccountDao() }
    val secureStorage = remember { SecureStorage(context) }
    val coroutineScope = rememberCoroutineScope()

    val accounts by dao.getAllAccounts().collectAsState(initial = emptyList())

    var codes by remember { mutableStateOf(mapOf<Int, String>()) }
    var secondsMap by remember { mutableStateOf(mapOf<Int, Int>()) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }

    var showEditDialog by remember { mutableStateOf(false) }
    var editAccountId by remember { mutableIntStateOf(0) }
    var editAccountName by remember { mutableStateOf("") }
    var editAccountIssuer by remember { mutableStateOf("") }
    var editAccountAlgorithm by remember { mutableStateOf("HmacSHA1") }
    var editAccountDigits by remember { mutableIntStateOf(6) }
    var editAccountPeriod by remember { mutableIntStateOf(30) }
    var editAccountSecretKey by remember { mutableStateOf("") }

    var showSettings by remember { mutableStateOf(false) }

    // Temporizador - cada cuenta usa su propio periodo
    LaunchedEffect(Unit) {
        while (true) {
            val currentAccounts = accounts
            val newCodes = mutableMapOf<Int, String>()
            val newSeconds = mutableMapOf<Int, Int>()
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
                        newSeconds[account.id] = gen.secondsRemaining()
                    } else {
                        newCodes[account.id] = "NO KEY"
                        newSeconds[account.id] = 0
                    }
                } catch (e: Exception) {
                    newCodes[account.id] = "ERROR"
                    newSeconds[account.id] = 0
                }
            }
            codes = newCodes
            secondsMap = newSeconds

            delay(1000L)
        }
    }

    // Diálogo para añadir cuenta manualmente
    if (showAddDialog) {
        AddAccountDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, issuer, secretKey, algorithm, digits, period ->
                coroutineScope.launch {
                    val account = TotpAccount(
                        name = name,
                        issuer = issuer,
                        algorithm = algorithm,
                        digits = digits,
                        period = period
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

    // Diálogo para editar cuenta
    if (showEditDialog) {
        EditAccountDialog(
            name = editAccountName,
            issuer = editAccountIssuer,
            secretKey = editAccountSecretKey,
            algorithm = editAccountAlgorithm,
            digits = editAccountDigits,
            period = editAccountPeriod,
            onDismiss = { showEditDialog = false },
            onSave = { name, issuer, secretKey, algorithm, digits, period ->
                val id = editAccountId
                coroutineScope.launch {
                    val updatedAccount = TotpAccount(
                        id = id,
                        name = name,
                        issuer = issuer,
                        algorithm = algorithm,
                        digits = digits,
                        period = period
                    )
                    dao.updateAccount(updatedAccount)
                    secureStorage.saveSecretKey(id, secretKey)
                }
                showEditDialog = false
            }
        )
    }

    // Pantalla de ajustes
    if (showSettings) {
        SettingsScreen(
            onBack = { showSettings = false }
        )
        return
    }

    // Pantalla del escáner QR
    if (showQrScanner) {
        QrScannerScreen(
            onQrScanned = { otpData ->
                try {
                    val decoded = com.example.totp.totp.Base32.decode(otpData.secretKey)
                    if (decoded.isEmpty() || otpData.secretKey.isBlank()) {
                        return@QrScannerScreen "QR inválido: la SecretKey está vacía o no es válida"
                    }
                    val generator = com.example.totp.totp.TotpGenerator()
                    generator.generateCode(otpData.secretKey)

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
                    return@QrScannerScreen null
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
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                },
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
                        secondsRemaining = secondsMap[account.id] ?: 0,
                        onDelete = {
                            coroutineScope.launch {
                                secureStorage.deleteSecretKey(account.id)
                                dao.deleteAccount(account)
                            }
                        },
                        onClick = {
                            editAccountId = account.id
                            editAccountName = account.name
                            editAccountIssuer = account.issuer
                            editAccountAlgorithm = account.algorithm
                            editAccountDigits = account.digits
                            editAccountPeriod = account.period
                            editAccountSecretKey = secureStorage.getSecretKey(account.id) ?: ""
                            showEditDialog = true
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
    onDelete: () -> Unit,
    onClick: () -> Unit
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
    onAdd: (name: String, issuer: String, secretKey: String, algorithm: String, digits: Int, period: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var issuer by remember { mutableStateOf("") }
    var secretKey by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var selectedAlgorithm by remember { mutableStateOf("HmacSHA1") }
    var digitsText by remember { mutableStateOf("6") }
    var periodText by remember { mutableStateOf("30") }

    var expandedAlgorithm by remember { mutableStateOf(false) }

    val algorithms = listOf("HmacSHA1", "HmacSHA256", "HmacSHA512")

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

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = when (selectedAlgorithm) {
                            "HmacSHA1" -> "SHA1"
                            "HmacSHA256" -> "SHA256"
                            "HmacSHA512" -> "SHA512"
                            else -> "SHA1"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Algoritmo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { expandedAlgorithm = true }
                    )
                    DropdownMenu(
                        expanded = expandedAlgorithm,
                        onDismissRequest = { expandedAlgorithm = false }
                    ) {
                        algorithms.forEach { algo ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (algo) {
                                            "HmacSHA1" -> "SHA1"
                                            "HmacSHA256" -> "SHA256"
                                            "HmacSHA512" -> "SHA512"
                                            else -> algo
                                        }
                                    )
                                },
                                onClick = {
                                    selectedAlgorithm = algo
                                    expandedAlgorithm = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = digitsText,
                        onValueChange = { digitsText = it.filter { c -> c.isDigit() } },
                        label = { Text("Dígitos") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = periodText,
                        onValueChange = { periodText = it.filter { c -> c.isDigit() } },
                        label = { Text("Periodo (s)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

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
                    val digits = digitsText.toIntOrNull()
                    val period = periodText.toIntOrNull()

                    if (secretKey.isBlank()) {
                        errorMessage = "La SecretKey es obligatoria"
                        showError = true
                    } else if (digits == null || digits < 4 || digits > 10) {
                        errorMessage = "Los dígitos deben ser entre 4 y 10"
                        showError = true
                    } else if (period == null || period < 1 || period > 300) {
                        errorMessage = "El periodo debe ser entre 1 y 300 segundos"
                        showError = true
                    } else {
                        try {
                            val decoded = com.example.totp.totp.Base32.decode(secretKey)
                            if (decoded.isEmpty()) {
                                errorMessage = "La SecretKey no es válida"
                                showError = true
                            } else {
                                val generator = com.example.totp.totp.TotpGenerator(
                                    digits = digits,
                                    period = period,
                                    algorithm = selectedAlgorithm
                                )
                                generator.generateCode(secretKey)
                                onAdd(name, issuer, secretKey, selectedAlgorithm, digits, period)
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

@Composable
fun EditAccountDialog(
    name: String,
    issuer: String,
    secretKey: String,
    algorithm: String,
    digits: Int,
    period: Int,
    onDismiss: () -> Unit,
    onSave: (name: String, issuer: String, secretKey: String, algorithm: String, digits: Int, period: Int) -> Unit
) {
    var editName by remember { mutableStateOf(name) }
    var editIssuer by remember { mutableStateOf(issuer) }
    var editSecretKey by remember { mutableStateOf(secretKey) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var selectedAlgorithm by remember { mutableStateOf(algorithm) }
    var digitsText by remember { mutableStateOf(digits.toString()) }
    var periodText by remember { mutableStateOf(period.toString()) }

    var expandedAlgorithm by remember { mutableStateOf(false) }

    val algorithms = listOf("HmacSHA1", "HmacSHA256", "HmacSHA512")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar cuenta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = editIssuer,
                    onValueChange = { editIssuer = it },
                    label = { Text("Servicio") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Cuenta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editSecretKey,
                    onValueChange = { editSecretKey = it.uppercase().replace(" ", "") },
                    label = { Text("SecretKey (Base32)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = when (selectedAlgorithm) {
                            "HmacSHA1" -> "SHA1"
                            "HmacSHA256" -> "SHA256"
                            "HmacSHA512" -> "SHA512"
                            else -> "SHA1"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Algoritmo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { expandedAlgorithm = true }
                    )
                    DropdownMenu(
                        expanded = expandedAlgorithm,
                        onDismissRequest = { expandedAlgorithm = false }
                    ) {
                        algorithms.forEach { algo ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (algo) {
                                            "HmacSHA1" -> "SHA1"
                                            "HmacSHA256" -> "SHA256"
                                            "HmacSHA512" -> "SHA512"
                                            else -> algo
                                        }
                                    )
                                },
                                onClick = {
                                    selectedAlgorithm = algo
                                    expandedAlgorithm = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = digitsText,
                        onValueChange = { digitsText = it.filter { c -> c.isDigit() } },
                        label = { Text("Dígitos") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = periodText,
                        onValueChange = { periodText = it.filter { c -> c.isDigit() } },
                        label = { Text("Periodo (s)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

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
                    val parsedDigits = digitsText.toIntOrNull()
                    val parsedPeriod = periodText.toIntOrNull()

                    if (secretKey.isBlank()) {
                        errorMessage = "La SecretKey es obligatoria"
                        showError = true
                    } else if (parsedDigits == null || parsedDigits < 4 || parsedDigits > 10) {
                        errorMessage = "Los dígitos deben ser entre 4 y 10"
                        showError = true
                    } else if (parsedPeriod == null || parsedPeriod < 1 || parsedPeriod > 300) {
                        errorMessage = "El periodo debe ser entre 1 y 300 segundos"
                        showError = true
                    } else {
                        try {
                            val decoded = com.example.totp.totp.Base32.decode(editSecretKey)
                            if (decoded.isEmpty()) {
                                errorMessage = "La SecretKey no es válida"
                                showError = true
                            } else {
                                val generator = com.example.totp.totp.TotpGenerator(
                                    digits = parsedDigits,
                                    period = parsedPeriod,
                                    algorithm = selectedAlgorithm
                                )
                                generator.generateCode(editSecretKey)
                                onSave(editName, editIssuer, editSecretKey, selectedAlgorithm, parsedDigits, parsedPeriod)
                            }
                        } catch (e: Exception) {
                            errorMessage = "La SecretKey no es Base32 válida (solo A-Z y 2-7)"
                            showError = true
                        }
                    }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}