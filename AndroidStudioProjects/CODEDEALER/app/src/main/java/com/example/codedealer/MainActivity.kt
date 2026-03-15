package com.example.codedealer

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundGray
                ) {

                }
            }
        }
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
fun LoginScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LogoC()
        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(value = "", onValueChange = {}, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = "", onValueChange = {}, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { navController.navigate("dashboard") },
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
fun RegisterScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar { navController.popBackStack() }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LogoC()
            Spacer(modifier = Modifier.height(48.dp))
            OutlinedTextField(value = "", onValueChange = {}, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = "", onValueChange = {}, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = true, onCheckedChange = {})
                Text("Remember me")
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { navController.navigate("dashboard") },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Text("Register")
            }
        }
    }
}

// --- PANTALLA: DASHBOARD ---
@Composable
fun DashboardScreen(navController: NavController) {
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

        // Lista de Propuestas
        LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
            items(4) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(120.dp).padding(vertical = 8.dp).clickable { navController.navigate("vista_propuesta") },
                    colors = CardDefaults.cardColors(containerColor = BlueLight),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.LightGray)) // Imagen placeholder
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("BUSCO PROGRAMADOR", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("ESPECIFICACION DE LA TAREA", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}