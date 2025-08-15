package com.organizen.app.home.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.organizen.app.auth.AuthViewModel

@Composable
fun TasksScreen(vm: AuthViewModel, onLogout: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome, ${vm.currentUser?.displayName ?: "User"}")
        Button(onClick = {
            vm.logout()
            onLogout()
        }) {
            Text("Log Out")
        }
    }
}

@Composable
fun HealthScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Health section")
    }
}

@Composable
fun ChatScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Chat section")
    }
}
