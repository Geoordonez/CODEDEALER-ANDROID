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
import coil.compose.AsyncImage
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.storage.FirebaseStorage

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
data class Mensaje(
    val id: String = "",
    val remitenteId: String = "",
    val texto: String = "",
    val timestamp: Long = 0L
)

data class Usuario(
    val username: String = "",
    val email: String = "",
    val fotoUrl: String = ""
)

fun obtenerChatId(uid1: String, uid2: String): String {
    return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
}

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

        composable("chat_usuario/{otherUserId}") { backStackEntry ->
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
            val currentUserId = auth.currentUser?.uid ?: ""
            UsuarioChatScreen(navController, currentUserId, otherUserId)
        }

        composable("vista_propuesta/{autorId}") { backStackEntry ->
            val autorId = backStackEntry.arguments?.getString("autorId") ?: ""
            VistaPropuestaScreen(navController, autorId)
        }

        composable("dashboard") { DashboardScreen(navController) }

        composable("chats") { ChatsScreen(navController, auth) }
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

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val userMap = mapOf(
        "username" to username,
        "email" to email,
        "fotoUrl" to "https://i.pravatar.cc/150?img=3",
        "rol" to "usuario",
        "fecha_registro" to System.currentTimeMillis().toString()
    )

    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar { navController.popBackStack() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            LogoC()
            Spacer(modifier = Modifier.height(48.dp))

            // 👤 USERNAME
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Nombre de usuario") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 📧 EMAIL (ESTO FALTABA)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 🔒 PASSWORD
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {

                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->

                                if (task.isSuccessful) {

                                    val userId = auth.currentUser?.uid

                                    val userMap = mapOf(
                                        "username" to username,
                                        "email" to email,
                                        "rol" to "usuario",
                                        "fecha_registro" to System.currentTimeMillis().toString()
                                    )

                                    if (userId != null) {
                                        val database = FirebaseDatabase.getInstance().reference

                                        database.child("Usuarios").child(userId).setValue(userMap)
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "¡Usuario creado!", Toast.LENGTH_SHORT).show()
                                                navController.navigate("dashboard") {
                                                    popUpTo("login") { inclusive = true }
                                                }
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(context, "Error al guardar datos", Toast.LENGTH_LONG).show()
                                            }
                                    }

                                } else {
                                    Toast.makeText(
                                        context,
                                        "Error: ${task.exception?.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }

                    } else {
                        Toast.makeText(context, "Llena todos los campos", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
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

    var listaPropuestas by remember { mutableStateOf<List<Propuesta>>(emptyList()) }
    var usuariosMap by remember { mutableStateOf<Map<String, Usuario>>(emptyMap()) }
    var searchText by remember { mutableStateOf("") }

    val context = LocalContext.current

    // 🔥 CARGAR PROPUESTAS
    LaunchedEffect(Unit) {
        val db = FirebaseDatabase.getInstance().getReference("Propuestas")

        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val temp = mutableListOf<Propuesta>()

                for (hijo in snapshot.children) {
                    val propuesta = hijo.getValue(Propuesta::class.java)
                    if (propuesta != null) temp.add(propuesta)
                }

                listaPropuestas = temp.reversed()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 🔥 CARGAR USUARIOS
    LaunchedEffect(Unit) {
        val db = FirebaseDatabase.getInstance().getReference("Usuarios")

        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val tempMap = mutableMapOf<String, Usuario>()

                for (userSnap in snapshot.children) {
                    val uid = userSnap.key ?: continue
                    val user = userSnap.getValue(Usuario::class.java)

                    if (user != null) {
                        tempMap[uid] = user
                    }
                }

                usuariosMap = tempMap
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    val propuestasFiltradas = listaPropuestas.filter {
        it.titulo.contains(searchText, true) ||
                it.descripcion.contains(searchText, true)
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // 🔝 HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 👤 PERFIL (YA NO GRIS SI FALLA)
            val currentUser = FirebaseAuth.getInstance().currentUser?.uid
            val miUsuario = usuariosMap[currentUser]

            AsyncImage(
                model = miUsuario?.fotoUrl ?: "https://i.pravatar.cc/150?u=default",
                contentDescription = "Perfil",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { navController.navigate("mis_propuestas") },
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )

            // 🔍 BUSCADOR
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Buscar tarea") },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(25.dp)
            )

            IconButton(onClick = { navController.navigate("chats") }) {
                Icon(Icons.Default.Email, contentDescription = null, tint = BluePrimary)
            }

            IconButton(onClick = { navController.navigate("publicar") }) {
                Icon(Icons.Default.Add, contentDescription = null, tint = BluePrimary)
            }
        }

        // 🔥 LISTA
        LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {

            if (propuestasFiltradas.isEmpty()) {
                item {
                    Text(
                        "No hay resultados",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray
                    )
                }
            } else {

                items(propuestasFiltradas.size) { index ->

                    val propuesta = propuestasFiltradas[index]
                    val user = usuariosMap[propuesta.autorId]

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(vertical = 8.dp)
                            .clickable {
                                navController.navigate("vista_propuesta/${propuesta.autorId}")
                            },
                        colors = CardDefaults.cardColors(containerColor = BlueLight),
                        shape = RoundedCornerShape(16.dp)
                    ) {

                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // 👤 FOTO SEGURA
                            AsyncImage(
                                model = user?.fotoUrl ?: "https://i.pravatar.cc/150?u=${propuesta.autorId}",
                                contentDescription = null,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {

                                Text(
                                    propuesta.titulo.uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    propuesta.descripcion,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    maxLines = 2
                                )
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

    val userId = auth.currentUser?.uid ?: ""
    var usuario by remember { mutableStateOf<Usuario?>(null) }
    var misPropuestas by remember { mutableStateOf<List<Propuesta>>(emptyList()) }

    val context = LocalContext.current

    // 🔥 SELECTOR DE IMAGEN
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->

        uri?.let {
            val storageRef = FirebaseStorage.getInstance()
                .reference
                .child("profile_images/$userId.jpg")

            storageRef.putFile(it)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->

                        FirebaseDatabase.getInstance()
                            .getReference("Usuarios")
                            .child(userId)
                            .child("fotoUrl")
                            .setValue(downloadUrl.toString())

                        Toast.makeText(context, "Foto actualizada", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error al subir imagen", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // 🔥 CARGAR USUARIO
    LaunchedEffect(userId) {
        val db = FirebaseDatabase.getInstance()
            .getReference("Usuarios")
            .child(userId)

        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usuario = snapshot.getValue(Usuario::class.java)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 🔥 CARGAR MIS PROPUESTAS
    LaunchedEffect(userId) {
        val db = FirebaseDatabase.getInstance().getReference("Propuestas")

        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val temp = mutableListOf<Propuesta>()

                for (hijo in snapshot.children) {
                    val propuesta = hijo.getValue(Propuesta::class.java)

                    if (propuesta != null && propuesta.autorId == userId) {
                        temp.add(propuesta)
                    }
                }

                misPropuestas = temp.reversed()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Column(modifier = Modifier.fillMaxSize()) {

        TopBar("Mi Perfil") { navController.popBackStack() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 🔥 FOTO PERFIL
            AsyncImage(
                model = usuario?.fotoUrl ?: "https://i.pravatar.cc/150",
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable { launcher.launch("image/*") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                usuario?.username ?: "Usuario",
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("MIS TAREAS", fontWeight = FontWeight.Bold, color = BluePrimary)

            Spacer(modifier = Modifier.height(16.dp))

            // 🔥 LISTA
            LazyColumn {

                if (misPropuestas.isEmpty()) {
                    item {
                        Text(
                            "No has publicado tareas aún",
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {

                    items(misPropuestas.size) { index ->
                        val propuesta = misPropuestas[index]

                        var showDialog by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = BlueLight),
                            shape = RoundedCornerShape(16.dp)
                        ) {

                            Column(modifier = Modifier.padding(16.dp)) {

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {

                                    Column(modifier = Modifier.weight(1f)) {

                                        Text(
                                            propuesta.titulo,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            propuesta.descripcion,
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }

                                    // 🗑️ BOTÓN ELIMINAR
                                    IconButton(onClick = {
                                        showDialog = true
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Eliminar",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // 🔥 CONFIRMAR ELIMINACIÓN
                        if (showDialog) {
                            AlertDialog(
                                onDismissRequest = { showDialog = false },
                                confirmButton = {
                                    TextButton(
                                        onClick = {

                                            val db = FirebaseDatabase.getInstance()
                                                .getReference("Propuestas")

                                            db.child(propuesta.id).removeValue()
                                                .addOnSuccessListener {
                                                    Toast.makeText(
                                                        context,
                                                        "Propuesta eliminada",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(
                                                        context,
                                                        "Error al eliminar",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }

                                            showDialog = false
                                        }
                                    ) {
                                        Text("Eliminar", color = Color.Red)
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { showDialog = false }
                                    ) {
                                        Text("Cancelar")
                                    }
                                },
                                title = { Text("Eliminar propuesta") },
                                text = { Text("¿Seguro que quieres eliminar esta tarea?") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VistaPropuestaScreen(navController: NavController, autorId: String) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar { navController.popBackStack() }

        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = BlueLight),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Imagen
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Título (puedes hacerlo dinámico después)
                Text(
                    "BUSCO PROGRAMADOR",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // Subtítulo
                Text(
                    "ESPECIALIZADO EN PYTHON",
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Descripción
                Text(
                    "especificacion de la tarea...",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                // 🔥 BOTÓN FUNCIONAL
                Button(
                    onClick = {
                        navController.navigate("chat_usuario/$autorId")
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(40.dp),
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
fun ChatsScreen(navController: NavController, auth: FirebaseAuth) {

    val currentUserId = auth.currentUser?.uid ?: ""

    var listaChats by remember { mutableStateOf<List<String>>(emptyList()) }
    var usuariosMap by remember { mutableStateOf<Map<String, Usuario>>(emptyMap()) }

    // 🔄 Leer chats
    LaunchedEffect(Unit) {
        val db = FirebaseDatabase.getInstance().getReference("Chats")

        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val temp = mutableListOf<String>()

                for (chatSnap in snapshot.children) {
                    val chatId = chatSnap.key ?: continue

                    if (chatId.contains(currentUserId)) {

                        val partes = chatId.split("_")
                        val otherUserId = if (partes[0] == currentUserId) {
                            partes[1]
                        } else {
                            partes[0]
                        }

                        temp.add(otherUserId)
                    }
                }

                listaChats = temp
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 🔄 Leer usuarios
    LaunchedEffect(Unit) {
        val db = FirebaseDatabase.getInstance().getReference("Usuarios")

        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val tempMap = mutableMapOf<String, Usuario>()

                for (userSnap in snapshot.children) {
                    val uid = userSnap.key ?: continue
                    val user = userSnap.getValue(Usuario::class.java)

                    if (user != null) {
                        tempMap[uid] = user
                    }
                }

                usuariosMap = tempMap
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Column(modifier = Modifier.fillMaxSize()) {

        TopBar("Mis Mensajes") { navController.popBackStack() }

        LazyColumn {

            // 🤖 CHAT IA
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BluePrimary)
                        .clickable { navController.navigate("chat") }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Face, contentDescription = null, tint = Color.White)

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text("ASISTENTE VIRTUAL", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Pregúntale lo que quieras", color = Color.White.copy(0.7f), fontSize = 12.sp)
                    }
                }

                Divider()
            }

            // 👤 CHATS
            items(listaChats.size) { index ->

                val otherUserId = listaChats[index]
                val user = usuariosMap[otherUserId]

                Column {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate("chat_usuario/$otherUserId")
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        AsyncImage(
                            model = user?.fotoUrl ?: "https://i.pravatar.cc/150",
                            contentDescription = null,
                            modifier = Modifier
                                .size(55.dp)
                                .clip(CircleShape)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {

                            Text(
                                user?.username ?: "Cargando...",
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "Toca para chatear",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        Text("Ahora", fontSize = 10.sp, color = Color.Gray)
                    }

                    // 🔥 DIVIDER PRO
                    Divider(
                        modifier = Modifier.padding(start = 80.dp),
                        thickness = 0.5.dp,
                        color = Color.LightGray
                    )
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

@Composable
fun UsuarioChatScreen(
    navController: NavController,
    currentUserId: String,
    otherUserId: String
) {

    var mensajeTexto by remember { mutableStateOf("") }
    var listaMensajes by remember { mutableStateOf<List<Mensaje>>(emptyList()) }

    val chatId = obtenerChatId(currentUserId, otherUserId)

    LaunchedEffect(chatId) {
        val db = FirebaseDatabase.getInstance().getReference("Chats").child(chatId)

        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val temp = mutableListOf<Mensaje>()
                for (hijo in snapshot.children) {
                    val msg = hijo.getValue(Mensaje::class.java)
                    if (msg != null) temp.add(msg)
                }
                listaMensajes = temp.sortedBy { it.timestamp }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Column(modifier = Modifier.fillMaxSize()) {

        TopBar("Chat") { navController.popBackStack() }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(12.dp)
        ) {
            items(listaMensajes.size) { i ->
                val msg = listaMensajes[i]
                val esMio = msg.remitenteId == currentUserId

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (esMio) Arrangement.End else Arrangement.Start
                ) {

                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .background(
                                if (esMio) BluePrimary else Color.LightGray,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Text(
                            msg.texto,
                            color = if (esMio) Color.White else Color.Black
                        )
                    }
                }
            }
        }

        Row(modifier = Modifier.padding(8.dp)) {

            BasicTextField(
                value = mensajeTexto,
                onValueChange = { mensajeTexto = it },
                modifier = Modifier
                    .weight(1f)
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .padding(12.dp)
            )

            IconButton(
                onClick = {
                    if (mensajeTexto.isNotEmpty()) {

                        val db = FirebaseDatabase.getInstance()
                            .getReference("Chats")
                            .child(chatId)

                        val id = db.push().key ?: return@IconButton

                        val msg = Mensaje(
                            id = id,
                            remitenteId = currentUserId,
                            texto = mensajeTexto,
                            timestamp = System.currentTimeMillis()
                        )

                        db.child(id).setValue(msg)

                        mensajeTexto = ""
                    }
                }
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = BluePrimary)
            }
        }
    }
}