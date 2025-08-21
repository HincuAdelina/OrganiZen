package com.organizen.app.home.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.organizen.app.auth.AuthViewModel
import com.organizen.app.home.data.TasksViewModel
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UpcomingTaskWidget(
    authVm: AuthViewModel,
    tasksVm: TasksViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val userId = authVm.currentUser?.uid ?: "guest"
    val tasks = tasksVm.tasksFor(userId)
    val upcoming = tasks
        .filter { !it.completed && !it.deadline.isBefore(LocalDate.now()) }
        .minByOrNull { it.deadline }

    upcoming?.let { task ->
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Next deadline", style = MaterialTheme.typography.titleMedium)
            TaskCard(
                task = task,
                onCheckedChange = { done -> tasksVm.setDone(userId, task.id, done) },
                onClick = {},
            )
        }
    }
}

