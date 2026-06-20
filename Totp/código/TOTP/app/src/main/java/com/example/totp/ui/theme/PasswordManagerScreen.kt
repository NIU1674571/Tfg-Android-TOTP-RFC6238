package com.example.totp.ui.theme

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.totp.model.PasswordEntry
import com.example.totp.model.TotpDatabase
import com.example.totp.totp.AuthStorage
import com.example.totp.totp.ImageStorage
import com.example.totp.totp.PasswordCryptoManager
import com.example.totp.totp.PasswordGenerator
import com.example.totp.totp.ThemeStorage
import kotlinx.coroutines.launch

/**
 * Pantalla principal del Gestor de Contraseñas.
 * Incluye listado con búsqueda, añadir, editar, eliminar y copiar al portapapeles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordManagerScreen(
    onBack: () -> Unit,
    onThemeChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember { TotpDatabase.getDatabase(context) }
    val passwordDao = remember { database.passwordEntryDao() }
    val crypto = remember { PasswordCryptoManager() }
    val themeStorage = remember { ThemeStorage(context) }
    val authStorage = remember { AuthStorage(context) }
    val coroutineScope = rememberCoroutineScope()

    // Estado de búsqueda
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Estado de diálogos
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editEntry by remember { mutableStateOf<PasswordEntry?>(null) }

    // Estado de pantallas internas (Ajustes y Apariencia)
    var showSettings by remember { mutableStateOf(false) }
    var showAppearance by remember { mutableStateOf(false) }

    // Estado del tutorial (-1 inactivo, 0..n paso activo)
    var tutorialStep by remember { mutableIntStateOf(-1) }
    val isTutorialActive = tutorialStep >= 0

    // Arrancar el tutorial automáticamente la primera vez que el usuario
    // entra en el Gestor de Contraseñas
    LaunchedEffect(Unit) {
        if (!authStorage.hasSeenPasswordManagerTutorial()) {
            tutorialStep = 0
            authStorage.setPasswordManagerTutorialSeen(true)
        }
    }

    // Si está en pantalla de Apariencia, mostrarla
    if (showAppearance) {
        // Botón atrás del sistema: volver a la pantalla anterior
        BackHandler { showAppearance = false }
        AppearanceScreen(
            onBack = { showAppearance = false },
            onThemeChanged = onThemeChanged
        )
        return
    }

    // Si está en pantalla de Ajustes, mostrarla
    if (showSettings) {
        // Botón atrás del sistema: volver a la pantalla anterior
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

    // Botón atrás del sistema en la pantalla principal:
    // si el tutorial está activo, lo cierra; si la búsqueda está activa,
    // cierra la búsqueda; si no, vuelve al Home
    BackHandler {
        when {
            isTutorialActive -> tutorialStep = -1
            isSearchActive -> {
                searchQuery = ""
                isSearchActive = false
            }
            else -> onBack()
        }
    }

    // Credenciales filtradas
    val entries by remember(searchQuery) {
        if (searchQuery.isBlank()) {
            passwordDao.getAllEntries()
        } else {
            passwordDao.searchEntries(searchQuery)
        }
    }.collectAsState(initial = emptyList())

    // Colores del tema
    val defaultBarColor = MaterialTheme.colorScheme.primaryContainer
    val topBarColor = remember(themeStorage.getTopBarColor(), defaultBarColor) {
        val hex = themeStorage.getTopBarColor()
        if (hex == "DEFAULT") defaultBarColor else hexToColor(hex)
    }
    val backgroundColor = remember(themeStorage.getBackgroundColor(), MaterialTheme.colorScheme.background) {
        val hex = themeStorage.getBackgroundColor()
        if (hex == "DEFAULT") Color.Unspecified else hexToColor(hex)
    }

    // Diálogo añadir credencial
    if (showAddDialog) {
        AddPasswordDialog(
            prefillForTutorial = (tutorialStep == 2 || tutorialStep == 3),
            onDismiss = { showAddDialog = false },
            onAdd = { serviceName, username, password, url ->
                coroutineScope.launch {
                    val encryptedPassword = crypto.encrypt(password)
                    val entry = PasswordEntry(
                        serviceName = serviceName,
                        username = username,
                        encryptedPassword = encryptedPassword,
                        url = url
                    )
                    passwordDao.insertEntry(entry)
                }
                showAddDialog = false
                // Tutorial paso 3 → 4: usuario añadió la credencial
                if (tutorialStep == 3) tutorialStep = 4
            }
        )
    }

    // Diálogo editar credencial
    if (showEditDialog && editEntry != null) {
        val decryptedPassword = remember(editEntry) {
            try {
                crypto.decrypt(editEntry!!.encryptedPassword)
            } catch (e: Exception) {
                ""
            }
        }
        EditPasswordDialog(
            entry = editEntry!!,
            decryptedPassword = decryptedPassword,
            onDismiss = {
                showEditDialog = false
                editEntry = null
                // Tutorial paso 5 → 6: usuario cerró el editor
                if (tutorialStep == 5) tutorialStep = 6
            },
            onSave = { serviceName, username, password, url, iconUri ->
                val currentEntry = editEntry!!
                coroutineScope.launch {
                    val encryptedPassword = crypto.encrypt(password)
                    val updatedEntry = currentEntry.copy(
                        serviceName = serviceName,
                        username = username,
                        encryptedPassword = encryptedPassword,
                        url = url,
                        iconUri = iconUri
                    )
                    passwordDao.updateEntry(updatedEntry)
                }
                showEditDialog = false
                editEntry = null
                // Tutorial paso 5 → 6: usuario guardó
                if (tutorialStep == 5) tutorialStep = 6
            }
        )
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            if (isSearchActive) {
                SearchTopBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onClose = {
                        searchQuery = ""
                        isSearchActive = false
                    },
                    topBarColor = topBarColor
                )
            } else {
                TopAppBar(
                    title = { Text("Gestor de Contraseñas") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = topBarColor
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showAddDialog = true
                    // Tutorial paso 1 → 2: usuario pulsó +
                    if (tutorialStep == 1) tutorialStep = 2
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir credencial")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank())
                            "No hay credenciales.\nAñade una con el botón +"
                        else
                            "No se encontraron resultados\npara \"$searchQuery\"",
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
                    items(entries, key = { it.id }) { entry ->
                        val defaultCardColor = MaterialTheme.colorScheme.surface
                        val cardColor = remember(themeStorage.getCardColor(), defaultCardColor) {
                            val hex = themeStorage.getCardColor()
                            if (hex == "DEFAULT") Color.Unspecified else hexToColor(hex)
                        }
                        PasswordCard(
                            entry = entry,
                            crypto = crypto,
                            cardColor = cardColor,
                            onClick = {
                                editEntry = entry
                                showEditDialog = true
                                // Tutorial paso 4 → 5: usuario pulsó la tarjeta
                                if (tutorialStep == 4) tutorialStep = 5
                            },
                            onDelete = {
                                coroutineScope.launch {
                                    ImageStorage.deleteImage(context, entry.id, prefix = "pwd_icon")
                                    passwordDao.deleteEntry(entry)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Tutorial overlay del Gestor de Contraseñas
        if (isTutorialActive) {
            PasswordManagerTutorialOverlay(
                currentStep = tutorialStep,
                onNextStep = {
                    tutorialStep++
                    if (tutorialStep >= 7) {
                        tutorialStep = -1
                    }
                },
                onSkip = {
                    tutorialStep = -1
                }
            )
        }
    }
}

// ==================== BARRA DE BÚSQUEDA ====================

/**
 * Barra superior con campo de búsqueda (OF-S4).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    topBarColor: Color
) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Buscar por servicio o usuario...") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar búsqueda")
            }
        },
        actions = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Limpiar")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = topBarColor
        )
    )
}

// ==================== TARJETA DE CREDENCIAL ====================

/**
 * Tarjeta de credencial con icono, servicio, usuario, contraseña,
 * botón de copiar (OF-S5) y botón de eliminar.
 */
@Composable
private fun PasswordCard(
    entry: PasswordEntry,
    crypto: PasswordCryptoManager,
    cardColor: Color = Color.Unspecified,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showPassword by remember { mutableStateOf(false) }

    // Descifrar la contraseña solo cuando se necesita mostrar o copiar
    val decryptedPassword = remember(entry.encryptedPassword, showPassword) {
        if (showPassword) {
            try {
                crypto.decrypt(entry.encryptedPassword)
            } catch (e: Exception) {
                "Error al descifrar"
            }
        } else null
    }

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
                // Icono del servicio (reutiliza ServiceIcon del módulo TOTP)
                ServiceIcon(
                    issuer = entry.serviceName,
                    customImageUri = entry.iconUri,
                    size = 44
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Nombre del servicio y usuario
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.serviceName.ifBlank { "Sin nombre" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (entry.username.isNotBlank()) {
                        Text(
                            text = entry.username,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                        contentDescription = "Eliminar credencial",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fila de contraseña: mostrar/ocultar + copiar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Contraseña (oculta o visible)
                Text(
                    text = if (showPassword && decryptedPassword != null)
                        decryptedPassword
                    else
                        "••••••••",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Botón mostrar/ocultar contraseña
                IconButton(
                    onClick = { showPassword = !showPassword },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (showPassword)
                            Icons.Default.Visibility
                        else
                            Icons.Default.VisibilityOff,
                        contentDescription = if (showPassword) "Ocultar" else "Mostrar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Botón copiar contraseña al portapapeles (OF-S5)
                IconButton(
                    onClick = {
                        try {
                            val password = crypto.decrypt(entry.encryptedPassword)
                            val clipboard = context.getSystemService(
                                android.content.ClipboardManager::class.java
                            )
                            val clip = android.content.ClipData.newPlainText(
                                "Password", password
                            )
                            clipboard?.setPrimaryClip(clip)
                            Toast.makeText(context, "Contraseña copiada", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error al copiar", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copiar contraseña",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // URL si existe
            if (entry.url.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = entry.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ==================== DIÁLOGO AÑADIR ====================

/**
 * Diálogo para añadir una nueva credencial.
 * Si prefillForTutorial es true, los campos se rellenan con datos de ejemplo
 * para que el usuario solo tenga que pulsar "Añadir" durante el tutorial.
 */
@Composable
private fun AddPasswordDialog(
    onDismiss: () -> Unit,
    prefillForTutorial: Boolean = false,
    onAdd: (serviceName: String, username: String, password: String, url: String) -> Unit
) {
    var serviceName by remember {
        mutableStateOf(if (prefillForTutorial) "Google" else "")
    }
    var username by remember {
        mutableStateOf(if (prefillForTutorial) "ejemplo@gmail.com" else "")
    }
    var password by remember {
        mutableStateOf(if (prefillForTutorial) "MiContraseña123" else "")
    }
    var url by remember {
        mutableStateOf(if (prefillForTutorial) "https://google.com" else "")
    }
    var showPassword by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

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
        title = { Text("Añadir credencial") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = serviceName,
                    onValueChange = { serviceName = it },
                    label = { Text("Servicio (ej: Google)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuario o email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    visualTransformation = if (showPassword)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword)
                                    Icons.Default.Visibility
                                else
                                    Icons.Default.VisibilityOff,
                                contentDescription = if (showPassword) "Ocultar" else "Mostrar"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Generador de contraseñas (OF-S3)
                PasswordGeneratorSection(
                    onPasswordGenerated = {
                        password = it
                        showPassword = true
                    }
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL (opcional)") },
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
            Button(
                onClick = {
                    when {
                        serviceName.isBlank() -> {
                            errorMessage = "El servicio es obligatorio"
                            showError = true
                        }
                        password.isBlank() -> {
                            errorMessage = "La contraseña es obligatoria"
                            showError = true
                        }
                        else -> {
                            showError = false
                            onAdd(serviceName, username, password, url)
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

// ==================== DIÁLOGO EDITAR ====================

/**
 * Diálogo para editar una credencial existente.
 * Incluye selector de icono personalizado para el servicio.
 */
@Composable
private fun EditPasswordDialog(
    entry: PasswordEntry,
    decryptedPassword: String,
    onDismiss: () -> Unit,
    onSave: (serviceName: String, username: String, password: String, url: String, iconUri: String?) -> Unit
) {
    var serviceName by remember { mutableStateOf(entry.serviceName) }
    var username by remember { mutableStateOf(entry.username) }
    var password by remember { mutableStateOf(decryptedPassword) }
    var url by remember { mutableStateOf(entry.url) }
    var iconUri by remember { mutableStateOf(entry.iconUri) }
    var showPassword by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val themeStorage = remember { ThemeStorage(context) }
    val defaultSurfaceColor = MaterialTheme.colorScheme.surface
    val dialogCardColor = remember(themeStorage.getCardColor(), defaultSurfaceColor) {
        val hex = themeStorage.getCardColor()
        if (hex == "DEFAULT") defaultSurfaceColor else hexToColor(hex)
    }

    // Selector de imagen desde galería
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageStorage.saveImageFromUri(
                context, uri, entry.id, prefix = "pwd_icon"
            )
            if (savedPath != null) {
                iconUri = savedPath
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogCardColor,
        title = { Text("Editar credencial") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = serviceName,
                    onValueChange = { serviceName = it },
                    label = { Text("Servicio") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Selector de icono personalizado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ServiceIcon(
                        issuer = serviceName,
                        customImageUri = iconUri,
                        size = 48
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { imageLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cambiar icono")
                        }
                        if (iconUri != null) {
                            TextButton(
                                onClick = {
                                    ImageStorage.deleteImage(context, entry.id, prefix = "pwd_icon")
                                    iconUri = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Usar icono por defecto")
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuario o email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    visualTransformation = if (showPassword)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword)
                                    Icons.Default.Visibility
                                else
                                    Icons.Default.VisibilityOff,
                                contentDescription = if (showPassword) "Ocultar" else "Mostrar"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Generador de contraseñas (OF-S3)
                PasswordGeneratorSection(
                    onPasswordGenerated = {
                        password = it
                        showPassword = true
                    }
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL (opcional)") },
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
            Button(
                onClick = {
                    when {
                        serviceName.isBlank() -> {
                            errorMessage = "El servicio es obligatorio"
                            showError = true
                        }
                        password.isBlank() -> {
                            errorMessage = "La contraseña es obligatoria"
                            showError = true
                        }
                        else -> {
                            showError = false
                            onSave(serviceName, username, password, url, iconUri)
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

// ==================== GENERADOR DE CONTRASEÑAS ====================

/**
 * Sección desplegable con el generador de contraseñas configurable (OF-S3).
 * Permite configurar: longitud, mayúsculas, minúsculas, números y caracteres especiales.
 * Se integra dentro de los diálogos de añadir y editar credenciales.
 */
@Composable
private fun PasswordGeneratorSection(
    onPasswordGenerated: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var lengthText by remember { mutableStateOf("16") }
    var includeUppercase by remember { mutableStateOf(true) }
    var includeLowercase by remember { mutableStateOf(true) }
    var includeNumbers by remember { mutableStateOf(true) }
    var includeSpecial by remember { mutableStateOf(true) }

    val generator = remember { PasswordGenerator() }

    Column {
        // Botón para expandir/colapsar el generador
        OutlinedButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (expanded) "Ocultar generador" else "Generar contraseña")
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))

            // Campo de texto para la longitud
            OutlinedTextField(
                value = lengthText,
                onValueChange = { newValue ->
                    // Solo permitir dígitos
                    if (newValue.all { it.isDigit() } && newValue.length <= 3) {
                        lengthText = newValue
                    }
                },
                label = { Text("Longitud (4-128)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Toggles de tipos de caracteres
            GeneratorToggle(
                label = "Mayúsculas (A-Z)",
                checked = includeUppercase,
                onCheckedChange = { includeUppercase = it },
                enabled = includeLowercase || includeNumbers || includeSpecial
            )
            GeneratorToggle(
                label = "Minúsculas (a-z)",
                checked = includeLowercase,
                onCheckedChange = { includeLowercase = it },
                enabled = includeUppercase || includeNumbers || includeSpecial
            )
            GeneratorToggle(
                label = "Números (0-9)",
                checked = includeNumbers,
                onCheckedChange = { includeNumbers = it },
                enabled = includeUppercase || includeLowercase || includeSpecial
            )
            GeneratorToggle(
                label = "Especiales (!@#\$%...)",
                checked = includeSpecial,
                onCheckedChange = { includeSpecial = it },
                enabled = includeUppercase || includeLowercase || includeNumbers
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Botón generar
            Button(
                onClick = {
                    val parsedLength = lengthText.toIntOrNull()?.coerceIn(4, 128) ?: 16
                    val generated = generator.generate(
                        length = parsedLength,
                        includeUppercase = includeUppercase,
                        includeLowercase = includeLowercase,
                        includeNumbers = includeNumbers,
                        includeSpecial = includeSpecial
                    )
                    onPasswordGenerated(generated)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generar")
            }
        }
    }
}

/**
 * Fila con un toggle (switch) para las opciones del generador.
 * El parámetro enabled evita que se desactiven todos los tipos a la vez.
 */
@Composable
private fun GeneratorToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
        Switch(
            checked = checked,
            onCheckedChange = {
                // Solo permitir desactivar si hay al menos otro tipo activo
                if (checked && !enabled) return@Switch
                onCheckedChange(it)
            }
        )
    }
}