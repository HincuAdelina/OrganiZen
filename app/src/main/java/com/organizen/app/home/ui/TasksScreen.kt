package com.organizen.app.home.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
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
import com.organizen.app.theme.DailyColor
import com.organizen.app.theme.LearningColor
import com.organizen.app.theme.SportColor
import java.time.ZoneId
import java.time.Instant
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
    val today = LocalDate.now()
    val overdue = today.isAfter(task.deadline) && !task.completed
    val dueToday = today.isEqual(task.deadline) && !task.completed
    val difficultyColor = when (task.difficulty) {
        Difficulty.EASY -> Color.Green
        Difficulty.MEDIUM -> Color.Yellow
        Difficulty.HARD -> Color.Red
    }
    val categoryIcons = mapOf(
        Category.SPORT to Icons.Default.FitnessCenter,
        Category.DAILY to Icons.Default.Check,
        Category.LEARNING to Icons.Default.School
    )
    val categoryColors = mapOf(
        Category.SPORT to SportColor,
        Category.DAILY to DailyColor,
        Category.LEARNING to LearningColor
    )
    val baseColor = if (task.completed) Color.LightGray else categoryColors[task.category] ?: MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp)
                .background(baseColor)
                .drawBehind {
                    if (overdue) {
                        val lineColor = Color.Red
                        val step = 20.dp.toPx()
                        var x = -size.height
                        while (x < size.width) {
                            drawLine(
                                lineColor,
                                Offset(x, 0f),
                                Offset(x + size.height, size.height),
                                strokeWidth = 4f
                            )
                            x += step
                        }
                    }
                }
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
                    categoryIcons[task.category]?.let { icon ->
                        Icon(icon, contentDescription = task.category.name, modifier = Modifier.size(24.dp).padding(end = 4.dp))
                    }
                }
            }
            Text(
                task.category.name.lowercase().replaceFirstChar { it.uppercase() },
                modifier = Modifier.align(Alignment.TopStart)
            )
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dueToday) {
                    Icon(Icons.Default.PriorityHigh, contentDescription = "Due today", tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                }
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
    var description by remember(task) { mutableStateOf(task?.description ?: "") }
    var estimated by remember(task) { mutableStateOf(task?.estimatedMinutes?.toString() ?: "") }
    var difficulty by remember(task) { mutableStateOf(task?.difficulty ?: Difficulty.EASY) }
    var selectedCategory by remember(task) { mutableStateOf(task?.category ?: Category.DAILY) }
    var deadline by remember(task) { mutableStateOf(task?.deadline ?: LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val minutes = estimated.toIntOrNull()
                if (description.isNotBlank() && minutes != null) {
                    onSave(
                        Task(
                            id = task?.id ?: System.currentTimeMillis(),
                            description = description,
                            difficulty = difficulty,
                            estimatedMinutes = minutes,
                            category = selectedCategory,
                            deadline = deadline,
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
                OutlinedTextField(
                    value = deadline.toString(),
                    onValueChange = {},
                    label = { Text("Deadline") },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
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
                Text("Category")
                Row {
                    Category.values().forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                }
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = deadline.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        deadline = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
