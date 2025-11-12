package com.example.composecontactos.ui.screens

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.composecontactos.data.Contact
import com.example.composecontactos.ui.components.ContactItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit),
    onNavigateToDetail: (Contact) -> Unit = {}
) {
    val context = LocalContext.current

    // Estados locales que manejan la UI y los datos
    // remember + mutableStateOf = estado que persiste entre recomposiciones
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) } // Lista de contactos cargados
    var isLoading by remember { mutableStateOf(false) } // Indica si está cargando (muestra spinner)
    var hasPermission by remember { mutableStateOf(false) } // Si tenemos permisos de contactos
    var errorMessage by remember { mutableStateOf<String?>(null) } // Mensaje de error actual (null = sin error)

    // Función para cargar contactos usando corrutinas
    suspend fun loadContacts() {
        // Marcar que iniciamos la carga - esto actualiza la UI para mostrar el loading
        isLoading = true
        errorMessage = null

        try {
            // withContext(Dispatchers.IO) ejecuta esta operación en un hilo de background
            // Esto es CRUCIAL para no bloquear el hilo principal (UI)
            val contactsList = withContext(Dispatchers.IO) {
                // Lista temporal para almacenar los contactos mientras los leemos
                val contacts = mutableListOf<Contact>()

                // ContentResolver es la API de Android para acceder a datos del sistema
                val contentResolver: ContentResolver = context.contentResolver

                // Query a la base de datos de contactos del sistema
                // ContactsContract.CommonDataKinds.Phone.CONTENT_URI = tabla de números de teléfono
                val cursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI, // URI de la tabla de números
                    arrayOf(
                        // Columnas que queremos obtener de cada contacto
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,    // ID único del contacto
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, // Nombre a mostrar
                        ContactsContract.CommonDataKinds.Phone.NUMBER        // Número de teléfono
                    ),
                    null, // selection (WHERE clause) - null = obtener todos
                    null, // selectionArgs - argumentos para el WHERE
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC" // ORDER BY nombre
                )

                // Cursor es como un puntero que navega por los resultados de la query
                cursor?.use { // use() garantiza que se cierre el cursor automáticamente
                    // Obtener índices de las columnas que necesitamos
                    val idColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val phoneColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    // Iterar por cada fila de resultados
                    while (it.moveToNext()) {
                        // Extraer datos de la fila actual
                        val id = it.getString(idColumn)
                        val name = it.getString(nameColumn) ?: "Sin nombre" // fallback si es null
                        val phoneNumber = it.getString(phoneColumn)

                        // Crear objeto Contact y agregarlo a la lista
                        contacts.add(
                            Contact(
                                id = id,
                                name = name,
                                phoneNumber = phoneNumber
                            )
                        )
                    }
                }

                // Eliminar duplicados basándose en el ID (un contacto puede tener múltiples números)
                contacts.distinctBy { it.id }
            }

            // Actualizar el estado con los contactos cargados - esto dispara recomposición
            contacts = contactsList
            isLoading = false // Marcar que terminamos la carga
        } catch (e: Exception) {
            // En caso de error, mostrar mensaje y parar loading
            isLoading = false
            errorMessage = "Error al cargar contactos: ${e.message}"
        }
    }

    // Launcher para solicitar permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            hasPermission = true
        } else {
            errorMessage = "Se necesita permiso para acceder a los contactos"
        }
    }

    // LaunchedEffect se ejecuta cuando el composable se crea por primera vez
    // Unit como key significa "ejecutar solo una vez al inicializar"
    LaunchedEffect(Unit) {
        // Verificar si ya tenemos permisos para leer contactos
        hasPermission = context.checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED

        // Si ya tenemos permisos, cargar contactos inmediatamente
        if (hasPermission) {
            loadContacts()
        }
    }

    // LaunchedEffect que se ejecuta cada vez que cambia hasPermission
    // Esto maneja el caso cuando el usuario otorga permisos
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            loadContacts()
        }
    }

    // LaunchedEffect que se ejecuta cuando cambia errorMessage
    // Esto permite recargar automáticamente cuando se limpia un error
    LaunchedEffect(errorMessage) {
        // Solo recargar si: tenemos permisos, no hay error, lista vacía y no está cargando
        if (hasPermission && errorMessage == null && contacts.isEmpty() && !isLoading) {
            loadContacts()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Contactos") },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // Lógica condicional para mostrar diferentes estados de la UI
            // when evalúa en orden: la primera condición verdadera determina qué se muestra
            when {
                !hasPermission -> {
                    // ESTADO 1: Sin permisos - mostrar pantalla de solicitud
                    PermissionRequestScreen {
                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                }

                isLoading -> {
                    // ESTADO 2: Cargando - mostrar spinner y mensaje
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Cargando contactos...")
                        }
                    }
                }

                errorMessage != null -> {
                    // ESTADO 3: Error - mostrar mensaje de error y botón reintentar
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "Error desconocido",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    // Limpiar error triggerea LaunchedEffect para recargar
                                    errorMessage = null
                                }
                            ) {
                                Text("Reintentar")
                            }
                        }
                    }
                }

                contacts.isEmpty() -> {
                    // ESTADO 4: Sin contactos - mostrar mensaje informativo
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron contactos",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    // ESTADO 5: Éxito - mostrar lista de contactos
                    // LazyColumn es una lista optimizada que solo renderiza elementos visibles
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = contacts,
                            key = { contact -> contact.id } // Key única para optimizar recomposición
                        ) { contact ->
                            ContactItem(
                                contact = contact,
                                onClick = { onNavigateToDetail(contact) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRequestScreen(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Permisos Necesarios",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Esta aplicación necesita acceso a tus contactos para mostrar la lista.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRequestPermission
            ) {
                Text("Otorgar Permisos")
            }
        }
    }
}