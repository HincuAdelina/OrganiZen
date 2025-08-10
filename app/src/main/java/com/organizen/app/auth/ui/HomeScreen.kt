package com.organizen.app.auth.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.organizen.app.auth.AuthViewModel

@Composable
fun HomeScreen(vm: AuthViewModel, onLogout: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome, ${vm.currentUser?.email ?: "User"}")
        Button(onClick = {
            vm.logout()
            onLogout()
        }) {
            Text("Log Out")
        }
    }
}
