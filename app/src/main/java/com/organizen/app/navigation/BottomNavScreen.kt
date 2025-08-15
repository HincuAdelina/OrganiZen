package com.organizen.app.navigation

sealed class BottomNavScreen(val route: String, val label: String) {
    object Tasks : BottomNavScreen("tasks", "Tasks")
    object Health : BottomNavScreen("health", "Health")
    object Chat : BottomNavScreen("chat", "Chat")
}
