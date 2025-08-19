package com.organizen.app.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.organizen.app.auth.AuthViewModel
import com.organizen.app.home.ui.HealthSection
import com.organizen.app.home.ui.TasksScreen
import com.organizen.app.home.ui.ProfileDrawerContent
import com.organizen.app.navigation.BottomNavScreen
import com.organizen.app.R
import com.organizen.app.home.ui.ChatSection
import kotlinx.coroutines.launch
import com.organizen.app.home.data.ChatViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: AuthViewModel, onLogout: () -> Unit) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val chatViewModel: ChatViewModel = viewModel()
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                ProfileDrawerContent(vm, onLogout = onLogout) {
                    scope.launch { drawerState.close() }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Image(painterResource(R.drawable.organizen_icon), contentDescription = "Profile")
                        }
                    }
                )
            },
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
                            icon = {},
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
                composable(BottomNavScreen.Tasks.route) { TasksScreen(vm) }
                composable(BottomNavScreen.Health.route) { HealthSection(vm, navController, chatViewModel) }
                composable(BottomNavScreen.Chat.route) { ChatSection(chatViewModel = chatViewModel) }
            }
        }
    }
}
