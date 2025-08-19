package com.organizen.app.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.organizen.app.auth.AuthViewModel
import com.organizen.app.home.data.ChatViewModel
import com.organizen.app.home.data.HealthViewModel
import com.organizen.app.home.data.TasksViewModel
import com.organizen.app.home.data.Task
import com.organizen.app.navigation.BottomNavScreen
import java.time.LocalDate
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HealthSection(
    authVm: AuthViewModel,
    navController: NavController,
    chatViewModel: ChatViewModel,
    vm: HealthViewModel = viewModel(),
    tasksVm: TasksViewModel = viewModel()
) {
    if (vm.steps == null || vm.sleepHours == null) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        val userId = authVm.currentUser?.uid ?: "guest"
        val tasks = tasksVm.tasksFor(userId)
        val available = tasks.filter { !it.completed && !it.deadline.isBefore(LocalDate.now()) }
        val tired = vm.steps!! < 5000 || vm.sleepHours!! < 7.0
        val comparator = compareBy<Task> { it.difficulty.ordinal }
            .thenBy { it.estimatedMinutes }
        val recommended = if (tired) {
            available.sortedWith(comparator).firstOrNull()
        } else {
            available.sortedWith(comparator.reversed()).firstOrNull()
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Steps")
                    Text(vm.steps.toString())
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Sleep")
                    Text(String.format("%.1f h", vm.sleepHours))
                }
            }
            recommended?.let { task ->
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Recommended task")
                    TaskCard(
                        task = task,
                        onCheckedChange = { done -> tasksVm.setDone(userId, task.id, done) },
                        onClick = {}
                    )
                    Button(onClick = {
                        val prompt = if (tired) {
                            "Suggest a short relaxation exercise."
                        } else {
                            "Give me a productivity advice to stay focused."
                        }
                        chatViewModel.sendMessage(prompt)
                        navController.navigate(BottomNavScreen.Chat.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }) {
                        Text(if (tired) "Get relax recommendations" else "Get productivity advices")
                    }
                }
            }
        }
    }
}
