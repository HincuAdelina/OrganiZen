package com.organizen.app.auth.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.organizen.app.auth.AuthViewModel
import com.organizen.app.home.ui.ChatScreen
import com.organizen.app.home.ui.HealthScreen
import com.organizen.app.home.ui.TasksScreen
import com.organizen.app.navigation.BottomNavScreen

@Composable
fun HomeScreen(vm: AuthViewModel, onLogout: () -> Unit) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                val items = listOf(
                    BottomNavScreen.Tasks,
                    BottomNavScreen.Health,
                    BottomNavScreen.Chat
                )
                items.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text(screen.label) },
                        icon = {}
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavScreen.Tasks.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavScreen.Tasks.route) { TasksScreen(vm, onLogout) }
            composable(BottomNavScreen.Health.route) { HealthScreen() }
            composable(BottomNavScreen.Chat.route) { ChatScreen() }
        }
    }
}
