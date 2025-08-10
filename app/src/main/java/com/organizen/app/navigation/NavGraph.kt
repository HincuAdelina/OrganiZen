package com.organizen.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.organizen.app.auth.AuthViewModel
import com.organizen.app.auth.ui.HomeScreen
import com.organizen.app.auth.ui.LoginScreen
import com.organizen.app.auth.ui.RegisterScreen

@Composable
fun AppNavGraph(navController: NavHostController, vm: AuthViewModel) {
    val startDestination = if (vm.currentUser != null) Screen.Home.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(
                vm,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) { popUpTo(0) }
                },
                onGoToRegister = { navController.navigate(Screen.Register.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                vm,
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) { popUpTo(0) }
                },
                onGoToLogin = { navController.navigate(Screen.Login.route) }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(vm, onLogout = {
                navController.navigate(Screen.Login.route) { popUpTo(0) }
            })
        }
    }
}
