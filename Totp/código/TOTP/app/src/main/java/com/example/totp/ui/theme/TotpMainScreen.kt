package com.example.totp.ui.theme

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.totp.model.TotpAccount
import com.example.totp.model.TotpDatabase
import com.example.totp.totp.ImageStorage
import com.example.totp.totp.OtpAuthData
import com.example.totp.totp.OtpAuthParser
import com.example.totp.totp.QrImageAnalyzer
import com.example.totp.totp.SecureStorage
import com.example.totp.totp.ThemeStorage
import com.example.totp.totp.TotpGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TotpMainScreen(
    onThemeChanged: () -> Unit = {},
    showTutorialOnStart: Boolean = false,
    onTutorialShown: () -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember { TotpDatabase.getDatabase(context) }
    val dao = remember { database.totpAccountDao() }
    val secureStorage = remember { SecureStorage(context) }
    val themeStorage = remember { ThemeStorage(context) }
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
    var editAccountIconUri by remember { mutableStateOf<String?>(null) }

    var showSettings by remember { mutableStateOf(false) }
    var showAppearance by remember { mutableStateOf(false) }

    var showImageError by remember { mutableStateOf(false) }
    var imageErrorMessage by remember { mutableStateOf("") }

    var tutorialStep by remember { mutableIntStateOf(if (showTutorialOnStart) 0 else -1) }
    val isTutorialActive = tutorialStep >= 0

    val defaultBarColor = MaterialTheme.colorScheme.primaryContainer
    // Se añade defaultBarColor como clave para forzar la actualización al cambiar el tema (claro/oscuro)
    val topBarColor = remember(themeStorage.getTopBarColor(), defaultBarColor) {
        val hex = themeStorage.getTopBarColor()
        if (hex == "DEFAULT") defaultBarColor else hexToColor(hex)
    }

    val backgroundColor = remember(themeStorage.getBackgroundColor(), MaterialTheme.colorScheme.background) {
        val hex = themeStorage.getBackgroundColor()
        if (hex == "DEFAULT") Color.Unspecified else hexToColor(hex)
    }

    // Launcher para seleccionar imagen de la galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            QrImageAnalyzer.analyzeImageUri(
                context = context,
                uri = uri,
                onSuccess = { rawValue ->
                    val otpData = OtpAuthParser.parse(rawValue)
                    if (otpData != null) {
                        // Validar SecretKey
                        try {
                            val decoded = com.example.totp.totp.Base32.decode(otpData.secretKey)
                            if (decoded.isEmpty() || otpData.secretKey.isBlank()) {
                                imageErrorMessage = "QR inválido: la SecretKey está vacía o no es válida"
                                showImageError = true
                                return@analyzeImageUri
                            }
                            val generator = com.example.totp.totp.TotpGenerator()
                            generator.generateCode(otpData.secretKey)

                            // Guardar la cuenta
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
                        } catch (e: Exception) {
                            imageErrorMessage = "QR inválido: la SecretKey está vacía o no es válida"
                            showImageError = true
                        }
                    } else {
                        imageErrorMessage = "QR inválido: la SecretKey está vacía o no es válida"
                        showImageError = true
                    }
                },
                onError = { error ->
                    imageErrorMessage = error
                    showImageError = true
                }
            )
        }
    }

    // Temporizador
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
            prefillForTutorial = (tutorialStep == 3 || tutorialStep == 4),
            onAdd = { name: String, issuer: String, secretKey: String, algorithm: String, digits: Int, period: Int ->
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
                // Tutorial paso 4→5: usuario añadió la cuenta
                if (tutorialStep == 4) tutorialStep = 5
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
            iconUri = editAccountIconUri,
            accountId = editAccountId,
            onDismiss = {
                showEditDialog = false
                // Tutorial paso 6→7: usuario cerró el editor
                if (tutorialStep == 6) tutorialStep = 7
            },
            onSave = { name: String, issuer: String, secretKey: String, algorithm: String, digits: Int, period: Int, iconUri: String? ->
                val id = editAccountId
                coroutineScope.launch {
                    val updatedAccount = TotpAccount(
                        id = id,
                        name = name,
                        issuer = issuer,
                        algorithm = algorithm,
                        digits = digits,
                        period = period,
                        iconUri = iconUri
                    )
                    dao.updateAccount(updatedAccount)
                    secureStorage.saveSecretKey(id, secretKey)
                }
                showEditDialog = false
                // Tutorial paso 6→7: usuario guardó
                if (tutorialStep == 6) tutorialStep = 7
            }
        )
    }

    // Diálogo de error al importar QR desde galería
    if (showImageError) {
        AlertDialog(
            onDismissRequest = { showImageError = false },
            title = { Text("Error al importar QR") },
            text = { Text(imageErrorMessage) },
            confirmButton = {
                TextButton(onClick = { showImageError = false }) {
                    Text("Aceptar")
                }
            }
        )
    }

    // Pantalla de apariencia
    if (showAppearance) {
        BackHandler { 
            showAppearance = false
            showSettings = true
        }
        AppearanceScreen(
            onBack = { 
                showAppearance = false
                showSettings = true
            },
            onThemeChanged = onThemeChanged
        )
        return
    }

    // Pantalla de ajustes
    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(
            onBack = { showSettings = false },
            onAppearanceClick = {
                showSettings = false
                showAppearance = true
            },
            onTutorialClick = {
                showSettings = false
                tutorialStep = 0
            }
        )
        return
    }

    // Pantalla del escáner QR
    if (showQrScanner) {
        BackHandler { showQrScanner = false }
        QrScannerScreen(
            onQrScanned = { otpData: OtpAuthData ->
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
                    return@QrScannerScreen "QR inválido: la SecretKey está vacía o no es válida"
                }
            },
            onBack = { showQrScanner = false }
        )
        return
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("TOTP Authenticator") },
                actions = {
                    IconButton(onClick = { 
                        showSettings = true 
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarColor
                )
            )
        },
        floatingActionButton = {
            var showMenu by remember { mutableStateOf(false) }

            Box {
                FloatingActionButton(
                    onClick = {
                        showMenu = true
                        // Tutorial paso 1→2: usuario pulsó +
                        if (tutorialStep == 1) tutorialStep = 2
                    }
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
                            // Tutorial paso 2→3: usuario pulsó \"Introducir manualmente\"
                            if (tutorialStep == 2) tutorialStep = 3
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Escanear QR") },
                        onClick = {
                            showMenu = false
                            showQrScanner = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Importar QR desde galería") },
                        onClick = {
                            showMenu = false
                            galleryLauncher.launch("image/*")
                        }
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (accounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
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
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(accounts, key = { it.id }) { account ->
                        val defaultCardColor = MaterialTheme.colorScheme.surface
                        val cardColor = remember(themeStorage.getCardColor(), defaultCardColor) {
                            val hex = themeStorage.getCardColor()
                            if (hex == "DEFAULT") Color.Unspecified else hexToColor(hex)
                        }
                        TotpCard(
                            account = account,
                            code = codes[account.id] ?: "------",
                            secondsRemaining = secondsMap[account.id] ?: 0,
                            onDelete = {
                                coroutineScope.launch {
                                    ImageStorage.deleteImage(context, account.id)
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
                                editAccountIconUri = account.iconUri
                                showEditDialog = true
                                // Tutorial paso 5→6: usuario pulsó la tarjeta
                                if (tutorialStep == 5) tutorialStep = 6
                            },
                            cardColor = cardColor
                        )
                    }
                }
            }
        }

        // Tutorial overlay
        if (isTutorialActive) {
            TutorialOverlay(
                currentStep = tutorialStep,
                onNextStep = {
                    tutorialStep++
                    if (tutorialStep >= 8) {
                        tutorialStep = -1
                        onTutorialShown()
                    }
                },
                onSkip = {
                    tutorialStep = -1
                    onTutorialShown()
                }
            )
        }
    }
}

@Composable
fun TotpCard(
    account: TotpAccount,
    code: String,
    secondsRemaining: Int,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    cardColor: Color = Color.Unspecified
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

    // Contexto para copiar al portapapeles
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (cardColor != Color.Unspecified)
            CardDefaults.cardColors(containerColor = cardColor)
        else CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Fila superior: icono + info + eliminar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono del servicio
                ServiceIcon(
                    issuer = account.issuer,
                    customImageUri = account.iconUri,
                    size = 44
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Nombre del servicio y cuenta
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (account.issuer.isNotBlank()) account.issuer else "Sin nombre",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (account.name.isNotBlank()) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Botón eliminar
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
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Código TOTP + copiar + temporizador
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Código TOTP
                Text(
                    text = code.chunked(3).joinToString(" "),
                    fontSize = when {
                        code.length > 8 -> 22.sp
                        code.length > 6 -> 26.sp
                        else -> 32.sp
                    },
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                // Botón copiar
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                        val clip = android.content.ClipData.newPlainText("TOTP Code", code)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copiar código",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Temporizador
                Text(
                    text = "${secondsRemaining}s",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = timerColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Info de parámetros
            Text(
                text = "${account.algorithm.replace("Hmac", "")} · ${account.digits} dígitos · ${account.period}s",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Barra de progreso
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
    prefillForTutorial: Boolean = false,
    onAdd: (name: String, issuer: String, secretKey: String, algorithm: String, digits: Int, period: Int) -> Unit
) {
    var name by remember { mutableStateOf(if (prefillForTutorial) "ejemplo@gmail.com" else "") }
    var issuer by remember { mutableStateOf(if (prefillForTutorial) "Google" else "") }
    var secretKey by remember { mutableStateOf(if (prefillForTutorial) "JBSWY3DPEHPK3PXP" else "") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var selectedAlgorithm by remember { mutableStateOf("HmacSHA1") }
    var digitsText by remember { mutableStateOf("6") }
    var periodText by remember { mutableStateOf("30") }

    var expandedAlgorithm by remember { mutableStateOf(false) }

    val algorithms = listOf("HmacSHA1", "HmacSHA256", "HmacSHA512")
    val context = LocalContext.current
    val themeStorage = remember { ThemeStorage(context) }
    val defaultSurfaceColor = MaterialTheme.colorScheme.surface
    val dialogCardColor = remember(themeStorage.getCardColor(), defaultSurfaceColor) {
        val hex = themeStorage.getCardColor()
        if (hex == "DEFAULT") defaultSurfaceColor else hexToColor(hex)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogCardColor,
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
            Button(
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
    iconUri: String?,
    accountId: Int,
    onDismiss: () -> Unit,
    onSave: (name: String, issuer: String, secretKey: String, algorithm: String, digits: Int, period: Int, iconUri: String?) -> Unit
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

    var editIconUri by remember { mutableStateOf(iconUri) }

    val context = LocalContext.current
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageStorage.saveImageFromUri(context, uri, accountId)
            if (savedPath != null) {
                editIconUri = savedPath
            }
        }
    }

    val algorithms = listOf("HmacSHA1", "HmacSHA256", "HmacSHA512")
    val themeStorage = remember { ThemeStorage(context) }
    val defaultSurfaceColor = MaterialTheme.colorScheme.surface
    val dialogCardColor = remember(themeStorage.getCardColor(), defaultSurfaceColor) {
        val hex = themeStorage.getCardColor()
        if (hex == "DEFAULT") defaultSurfaceColor else hexToColor(hex)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogCardColor,
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

                // Selector de imagen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ServiceIcon(
                        issuer = editIssuer,
                        customImageUri = editIconUri,
                        size = 48
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { imageLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cambiar icono")
                        }
                        if (editIconUri != null) {
                            TextButton(
                                onClick = { 
                                    ImageStorage.deleteImage(context, accountId)
                                    editIconUri = null 
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Usar icono por defecto")
                            }
                        }
                    }
                }

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
            Button(
                onClick = {
                    val parsedDigits = digitsText.toIntOrNull()
                    val parsedPeriod = periodText.toIntOrNull()

                    if (editSecretKey.isBlank()) {
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
                                onSave(editName, editIssuer, editSecretKey, selectedAlgorithm, parsedDigits, parsedPeriod, editIconUri)
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
