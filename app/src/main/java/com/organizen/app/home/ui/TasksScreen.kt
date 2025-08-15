package com.organizen.app.home.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.organizen.app.auth.AuthViewModel
import com.organizen.app.home.data.*
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(vm: AuthViewModel, tasksVm: TasksViewModel = viewModel()) {
    val userId = vm.currentUser?.uid ?: "guest"
    val tasks = tasksVm.tasksFor(userId)
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                val dismissState = rememberDismissState { value ->
                    if (value == DismissValue.DismissedToStart) {
                        tasksVm.removeTask(userId, task.id)
                    }
                    true
                }
                SwipeToDismiss(
                    state = dismissState,
                    directions = setOf(DismissDirection.EndToStart),
                    background = {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Red.copy(alpha = 0.3f))
                                .padding(16.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    },
                    dismissContent = {
                        TaskCard(
                            task = task,
                            onCheckedChange = { done -> tasksVm.setDone(userId, task.id, done) },
                            onClick = { editingTask = task; showDialog = true }
                        )
                    }
                )
            }
        }
    }

    if (showDialog) {
        TaskDialog(
            task = editingTask,
            onDismiss = { showDialog = false; editingTask = null },
            onSave = {
                tasksVm.upsertTask(userId, it)
                showDialog = false
                editingTask = null
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun TaskCard(task: Task, onCheckedChange: (Boolean) -> Unit, onClick: () -> Unit) {
    val overdue = task.deadline.isBefore(LocalDate.now()) && !task.completed
    val difficultyColor = when (task.difficulty) {
        Difficulty.EASY -> Color.Green
        Difficulty.MEDIUM -> Color.Yellow
        Difficulty.HARD -> Color.Red
    }
    val tagIcons = mapOf(
        Tag.RELAXARE to Icons.Default.SelfImprovement,
        Tag.INVATARE to Icons.Default.School,
        Tag.SPORT to Icons.Default.FitnessCenter
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        border = if (overdue) BorderStroke(1.dp, Color.Red) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (task.completed) Color.LightGray else MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp)
                .padding(8.dp)
        ) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            Column(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 48.dp, end = 80.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Circle, contentDescription = null, tint = difficultyColor, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        task.description,
                        color = when {
                            overdue -> Color.Red
                            task.completed -> Color.Gray
                            else -> Color.Unspecified
                        }
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row {
                    task.tags.forEach { tag ->
                        tagIcons[tag]?.let { icon ->
                            Icon(icon, contentDescription = tag.name, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                        }
                    }
                }
            }
            Text(
                task.tags.firstOrNull()?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "",
                modifier = Modifier.align(Alignment.TopStart)
            )
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(2.dp))
                Text("${task.estimatedMinutes}m", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                task.deadline.toString(),
                modifier = Modifier.align(Alignment.BottomEnd),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDialog(task: Task?, onDismiss: () -> Unit, onSave: (Task) -> Unit) {
    var description by remember { mutableStateOf(task?.description ?: "") }
    var estimated by remember { mutableStateOf(task?.estimatedMinutes?.toString() ?: "") }
    var deadline by remember { mutableStateOf(task?.deadline?.toString() ?: "") }
    var difficulty by remember { mutableStateOf(task?.difficulty ?: Difficulty.EASY) }
    val selectedTags = remember { mutableStateListOf<Tag>().apply { addAll(task?.tags ?: emptyList()) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val minutes = estimated.toIntOrNull()
                val date = runCatching { LocalDate.parse(deadline) }.getOrNull()
                if (description.isNotBlank() && minutes != null && date != null) {
                    onSave(
                        Task(
                            id = task?.id ?: System.currentTimeMillis(),
                            description = description,
                            difficulty = difficulty,
                            estimatedMinutes = minutes,
                            tags = selectedTags.toList(),
                            deadline = date,
                            completed = task?.completed ?: false
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text(if (task == null) "Add Task" else "Edit Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") }
                )
                TextField(
                    value = estimated,
                    onValueChange = { estimated = it },
                    label = { Text("Estimated minutes") }
                )
                TextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text("Deadline (YYYY-MM-DD)") }
                )
                Text("Difficulty")
                Row {
                    Difficulty.values().forEach { d ->
                        FilterChip(
                            selected = difficulty == d,
                            onClick = { difficulty = d },
                            label = { Text(d.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                }
                Text("Tags")
                Row {
                    Tag.values().forEach { tag ->
                        FilterChip(
                            selected = selectedTags.contains(tag),
                            onClick = {
                                if (selectedTags.contains(tag)) selectedTags.remove(tag) else selectedTags.add(tag)
                            },
                            label = { Text(tag.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                }
            }
        }
    )
}
