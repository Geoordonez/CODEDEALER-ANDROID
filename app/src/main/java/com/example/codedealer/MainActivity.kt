package com.example.codedealer

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// --- COLORES PRINCIPALES ---
val BluePrimary = Color(0xFF0055FF)
val BlueLight = Color(0xFF4D88FF)
val BackgroundGray = Color(0xFFF5F5F5)

data class Propuesta(
    val id: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val autorId: String = "",
    val fecha: String = ""
)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = Firebase.auth

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundGray
                ) {

                    AppNavigation(auth)
                }
            }
        }
    }
}

// ==========================================
// 1. NAVEGACIÓN PRINCIPAL (EL FLUJO DE TUS FLECHAS)
// ==========================================
@Composable
fun AppNavigation(auth: FirebaseAuth) {
    val navController = rememberNavController()
    val geminiVm: GeminiViewModel = viewModel()

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(navController, auth)
        }

        composable("register") {
            RegisterScreen(navController, auth)
        }

        composable("dashboard") { DashboardScreen(navController) }

        composable("chats") { ChatsScreen(navController) }
        // ¡Aquí está el cambio! Le pasamos el 'auth' a PublicarScreen
        composable("publicar") {
            PublicarScreen(navController, auth)
        }
        composable("mis_propuestas") { MisPropuestasScreen(navController, auth) }
        composable("chat") { ChatScreen(navController, geminiVm) }
    }
}
// Componente reutilizable: El Logo "C"
@Composable
fun LogoC() {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(BluePrimary),
        contentAlignment = Alignment.Center
    ) {
        Text("C", color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold)
    }
}

// Componente reutilizable: Barra superior con botón de regreso
@Composable
fun TopBar(title: String = "", onBackClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
        }
        if (title.isNotEmpty()) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

// ==========================================
// PANTALLAS DEL FLUJO
// ==========================================

// --- PANTALLA: LOGIN ---
@Composable
fun LoginScreen(navController: NavController, auth: FirebaseAuth) {
    // 1. Estados para guardar lo que el usuario escribe
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LogoC()
        Spacer(modifier = Modifier.height(48.dp))

        // 2. Campo de Email funcional
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 3. Campo de Password funcional
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))

        // 4. Botón con lógica de Firebase para iniciar sesión
        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, "¡Inicio de sesión exitoso!", Toast.LENGTH_SHORT).show()
                                navController.navigate("dashboard") {
                                    // Esto evita que al darle "atrás" vuelva al login
                                    popUpTo("login") { inclusive = true }
                                }
                            } else {
                                Toast.makeText(context, "Error: Verifica tus datos", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(context, "Por favor, llena todos los campos", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
        ) {
            Text("Sign In")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController.navigate("register") },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
        ) {
            Text("Register")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Forgot password?", color = BluePrimary, fontSize = 14.sp)
    }
}
// --- PANTALLA: REGISTER ---
@Composable
fun RegisterScreen(navController: NavController, auth: FirebaseAuth) {
    // 1. Aquí declaramos las variables que te marcaban error
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar { navController.popBackStack() }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LogoC()
            Spacer(modifier = Modifier.height(48.dp))

            // Campo de Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Campo de Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Botón con toda la lógica de Firebase integrada
            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val userId = auth.currentUser?.uid

                                    val userMap = mapOf(
                                        "email" to email,
                                        "rol" to "usuario",
                                        "fecha_registro" to System.currentTimeMillis().toString()
                                    )

                                    if (userId != null) {
                                        val database = FirebaseDatabase.getInstance().reference

                                        database.child("Usuarios").child(userId).setValue(userMap)
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "¡Usuario guardado en BD!", Toast.LENGTH_SHORT).show()
                                                navController.navigate("dashboard")
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context, "Error BD: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                    }
                                } else {
                                    Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        Toast.makeText(context, "Llena todos los campos", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Text("Register Now")
            }
        }
    }
}

// --- PANTALLA: DASHBOARD ---
@Composable
fun DashboardScreen(navController: NavController) {
    // 1. Estado para guardar la lista de propuestas que descargaremos de Firebase
    var listaPropuestas by remember { mutableStateOf<List<Propuesta>>(emptyList()) }

    // 2. Descargar los datos de Firebase en tiempo real
    LaunchedEffect(Unit) {
        val database = FirebaseDatabase.getInstance().getReference("Propuestas")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val propuestasTemp = mutableListOf<Propuesta>()
                // Recorremos cada propuesta guardada en la base de datos
                for (hijo in snapshot.children) {
                    val propuesta = hijo.getValue(Propuesta::class.java)
                    if (propuesta != null) {
                        propuestasTemp.add(propuesta)
                    }
                }
                // Las invertimos para que las más nuevas salgan hasta arriba
                listaPropuestas = propuestasTemp.reversed()
            }

            override fun onCancelled(error: DatabaseError) {
                // Si hay un error de conexión, no hacemos nada grave por ahora
            }
        })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Perfil
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray).clickable { navController.navigate("mis_propuestas") }
            )
            // Buscador
            OutlinedTextField(
                value = "", onValueChange = {}, placeholder = { Text("Buscar tarea") },
                modifier = Modifier.weight(1f).height(50.dp).padding(horizontal = 8.dp),
                shape = RoundedCornerShape(25.dp)
            )
            // Iconos
            IconButton(onClick = { navController.navigate("chats") }) {
                Icon(Icons.Default.Email, contentDescription = "Chats", tint = BluePrimary)
            }
            IconButton(onClick = { navController.navigate("publicar") }) {
                Icon(Icons.Default.Add, contentDescription = "Publicar", tint = BluePrimary)
            }
        }

        // 3. Lista de Propuestas REALES
        LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
            // Si no hay propuestas, podemos mostrar un mensajito
            if (listaPropuestas.isEmpty()) {
                item {
                    Text(
                        text = "Aún no hay propuestas publicadas. ¡Sé el primero!",
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            } else {
                // Aquí creamos una tarjeta por cada propuesta real en la BD
                items(listaPropuestas.size) { index ->
                    val propuesta = listaPropuestas[index]

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(vertical = 8.dp)
                            .clickable { navController.navigate("vista_propuesta") },
                        colors = CardDefaults.cardColors(containerColor = BlueLight),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.LightGray)) // Imagen placeholder
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                // Mostramos el TÍTULO real
                                Text(propuesta.titulo.uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                // Mostramos la DESCRIPCIÓN real (limitada a 2 líneas para que no se desborde)
                                Text(propuesta.descripcion, color = Color.White, fontSize = 12.sp, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }
    }
}
// --- PANTALLA: MIS PROPUESTAS ---
@Composable
fun MisPropuestasScreen(navController: NavController, auth: FirebaseAuth) {
    // 1. Estado para guardar solo MIS propuestas
    var misPropuestas by remember { mutableStateOf<List<Propuesta>>(emptyList()) }
    val userId = auth.currentUser?.uid // El ID del usuario actual

    // NUEVO: Necesitamos el contexto para mostrar el mensajito de "Eliminada"
    val context = LocalContext.current

    // 2. Descargar y filtrar datos de Firebase
    LaunchedEffect(userId) {
        if (userId != null) {
            val database = FirebaseDatabase.getInstance().getReference("Propuestas")

            database.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val propuestasTemp = mutableListOf<Propuesta>()

                    // Recorremos todas las propuestas
                    for (hijo in snapshot.children) {
                        val propuesta = hijo.getValue(Propuesta::class.java)

                        // FILTRO CLAVE: Solo agregamos si el autor es el usuario actual
                        if (propuesta != null && propuesta.autorId == userId) {
                            propuestasTemp.add(propuesta)
                        }
                    }
                    // Las invertimos para ver la más reciente primero
                    misPropuestas = propuestasTemp.reversed()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Manejo de errores
                }
            })
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar("Mi Perfil") { navController.popBackStack() }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.size(150.dp).clip(CircleShape).background(Color.LightGray)) // Avatar grande
            Spacer(modifier = Modifier.height(32.dp))

            Text("MIS TAREAS PUBLICADAS", fontWeight = FontWeight.Bold, color = BluePrimary)
            Spacer(modifier = Modifier.height(16.dp))

            // 3. Mostramos la lista o un mensaje si está vacía
            if (misPropuestas.isEmpty()) {
                Text("Aún no has publicado ninguna tarea.", color = Color.Gray, textAlign = TextAlign.Center)
            } else {

                // 🛑 SOLUCIÓN 1: Le agregamos .weight(1f) para darle un límite de tamaño seguro 🛑
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(misPropuestas.size) { index ->
                        val propuesta = misPropuestas[index]

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .background(BlueLight, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            val tituloSeguro = propuesta.titulo.toString().uppercase()

                            Text(tituloSeguro, color = Color.White, fontWeight = FontWeight.Bold)

                            IconButton(
                                onClick = {
                                    if (propuesta.id.isNotEmpty()) {
                                        // 1. Apuntamos a la carpeta Propuestas y luego al ID específico
                                        val databaseRef = FirebaseDatabase.getInstance().getReference("Propuestas").child(propuesta.id)

                                        // 2. Ejecutamos la eliminación
                                        databaseRef.removeValue()
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Tarea eliminada", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(context, "Error al eliminar", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
// --- PANTALLA: VISTA PROPUESTA ---
@Composable
fun VistaPropuestaScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar { navController.popBackStack() }
        Card(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = BlueLight),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.LightGray))
                Spacer(modifier = Modifier.height(16.dp))
                Text("BUSCO PROGRAMADOR", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("ESPECIALIZADO EN PYTHON", color = Color.White, fontSize = 16.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(32.dp))
                Text("especificacion de la tarea...", color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))

                Button(
                    onClick = { navController.navigate("chat") },
                    modifier = Modifier.fillMaxWidth(0.6f).height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text("CONTACTAR")
                }
            }
        }
    }
}

// --- PANTALLA: PUBLICAR ---
// --- PANTALLA: PUBLICAR ---
@Composable
fun PublicarScreen(navController: NavController, auth: FirebaseAuth) {
    // 1. Estados para guardar lo que el usuario escribe
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar("Nueva Propuesta") { navController.popBackStack() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(BlueLight, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {

            // 2. Campo para el Título
            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text("Título de la tarea", color = Color.White) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Campo para la Descripción (ocupa el resto de la pantalla)
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Describe los detalles de lo que buscas...", color = Color.White) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().weight(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Botón para enviar a la Base de Datos
            Button(
                onClick = {
                    val userId = auth.currentUser?.uid // Obtenemos el ID de quien publica

                    if (titulo.isNotEmpty() && descripcion.isNotEmpty() && userId != null) {
                        // Conectamos a la BD
                        val database = FirebaseDatabase.getInstance().reference

                        // Generamos un ID único para esta propuesta
                        val propuestaId = database.child("Propuestas").push().key

                        // Preparamos los datos
                        val propuestaMap = mapOf(
                            "id" to propuestaId,
                            "titulo" to titulo,
                            "descripcion" to descripcion,
                            "autorId" to userId,
                            "fecha" to System.currentTimeMillis().toString()
                        )

                        // Lo guardamos en la carpeta "Propuestas"
                        if (propuestaId != null) {
                            database.child("Propuestas").child(propuestaId).setValue(propuestaMap)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "¡Propuesta publicada!", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack() // Regresamos al dashboard
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                    } else {
                        Toast.makeText(context, "Llena todos los campos", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Text("PUBLICAR")
            }
        }
    }
}

// --- PANTALLA: CHATS (LISTA) ---
@Composable
fun ChatsScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Le ponemos título a la barra para saber dónde estamos
        TopBar("Mis Mensajes") { navController.popBackStack() }

        OutlinedTextField(
            value = "",
            onValueChange = {},
            placeholder = { Text("Buscar chat") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(25.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {

            // --- 1. BOTÓN ESPECIAL PARA LA IA (Siempre al principio) ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(BluePrimary, RoundedCornerShape(24.dp)) // Azul fuerte para resaltar
                        .padding(12.dp)
                        .clickable { navController.navigate("chat") }, // Manda a la pantalla de Gemini
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(Icons.Default.Face, contentDescription = null, tint = BluePrimary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("ASISTENTE VIRTUAL", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Pregúntale lo que quieras a Gemini", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }

            // --- 2. LISTA DE CHATS NORMALES (Usuarios) ---
            items(6) { indice ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(BlueLight, RoundedCornerShape(24.dp)) // Azul claro para usuarios
                        .padding(12.dp)
                        .clickable {
                            // Aquí no mandamos a "chat" (IA), por ahora no hace nada
                            // o podrías mandarlo a una pantalla de chat genérica
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.LightGray))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("USUARIO DE PRUEBA $indice", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- PANTALLA: CHAT (MENSAJES) ---
@Composable
fun ChatScreen(navController: NavController, geminiVm: GeminiViewModel) {
    // Observamos los estados del ViewModel
    val respuestaIA by geminiVm.respuestaIA.collectAsState()
    val estaCargando by geminiVm.estaCargando.collectAsState()

    // Estado para que el usuario pueda escribir
    var textoUsuario by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar("Chat con Gemini") { navController.popBackStack() }

        // Área de mensajes (Historial)
        Column(modifier = Modifier.weight(1f).padding(16.dp)) {
            // Burbuja de la IA
            Box(
                modifier = Modifier
                    .background(if (estaCargando) Color.Gray else BlueLight, RoundedCornerShape(12.dp))
                    .padding(16.dp)
                    .align(Alignment.Start)
            ) {
                Text(
                    text = if (estaCargando) "Gemini está escribiendo..." else respuestaIA,
                    color = Color.White
                )
            }
        }

        // Input de texto real
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color.White, RoundedCornerShape(25.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TextField para que el teclado funcione
            BasicTextField(
                value = textoUsuario,
                onValueChange = { textoUsuario = it },
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (textoUsuario.isEmpty()) Text("Pregúntale algo a la IA...", color = Color.Gray)
                    innerTextField()
                }
            )

            // Botón de enviar funcional
            IconButton(
                onClick = {
                    if (textoUsuario.isNotEmpty()) {
                        geminiVm.preguntarAGeminiConContexto(textoUsuario)
                        textoUsuario = "" // Limpiar el campo
                    }
                },
                enabled = !estaCargando && textoUsuario.isNotEmpty()
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Enviar",
                    tint = if (estaCargando) Color.Gray else BluePrimary
                )
            }
        }
    }
}