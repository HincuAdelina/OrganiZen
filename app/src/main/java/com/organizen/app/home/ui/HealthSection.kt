package com.organizen.app.home.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.organizen.app.navigation.BottomNavScreen
import java.time.LocalDate
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HealthSection(
    authVm: AuthViewModel,
    navController: NavController,
    chatViewModel: ChatViewModel,
    tasksVm: TasksViewModel,
    vm: HealthViewModel = viewModel(),
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
        // Consider the user tired only when today's sleep is low or they have already walked a lot
        val tired = vm.sleepHours!! < 7.0 || vm.steps!! > 5000
        val recommended = if (available.isNotEmpty()) {
            val easiest = available.minByOrNull { it.difficulty.ordinal }
            val shortest = available.minByOrNull { it.estimatedMinutes }
            val hardest = available.maxByOrNull { it.difficulty.ordinal }
            val longest = available.maxByOrNull { it.estimatedMinutes }
            val candidates = if (tired) {
                listOfNotNull(easiest, shortest)
            } else {
                listOfNotNull(hardest, longest)
            }.distinct()
            if (candidates.isNotEmpty()) candidates.random() else null
        } else null

        val sleepMinutes = (vm.sleepHours!! * 60).roundToInt()
        val sleepHoursPart = sleepMinutes / 60
        val sleepMinutesPart = sleepMinutes % 60

        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.DirectionsWalk, contentDescription = "Steps")
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Steps")
                        Text(vm.steps.toString())
                    }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Bedtime, contentDescription = "Sleep")
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Sleep")
                        Text("${sleepHoursPart}h ${sleepMinutesPart}m")
                    }
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
                        onClick = {},
                    )
                }
            }
            Button(
                onClick = {
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
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (tired) "Get relax recommendations" else "Get productivity advices")
            }
        }
    }
}
