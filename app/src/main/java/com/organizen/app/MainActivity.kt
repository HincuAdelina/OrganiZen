package com.organizen.app

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.organizen.app.auth.AuthViewModel
import com.organizen.app.navigation.AppNavGraph
import com.organizen.app.theme.OrganiZenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        // icon animation after splash screen
        splash.setOnExitAnimationListener { splashViewProvider ->
            val v = splashViewProvider.iconView
            v.animate()
                .scaleX(1.5f).scaleY(1.5f)
                .alpha(0f)
                .setDuration(320)
                .withEndAction { splashViewProvider.remove() }
                .start()
        }

        setContent {
            OrganiZenTheme {
                val navController = rememberNavController()
                val vm = AuthViewModel()
                AppNavGraph(navController, vm)
            }
        }
    }
}
