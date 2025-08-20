package com.organizen.app.navigation

sealed class BottomNavScreen(val route: String, val label: String) {
    object Tasks : BottomNavScreen("tasks", "Tasks")
    object Productivity : BottomNavScreen("productivity", "Productivity")
    object Chat : BottomNavScreen("chat", "Chat")
}
