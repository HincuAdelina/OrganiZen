package com.organizen.app.home.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FixedThreshold
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.organizen.app.auth.AuthViewModel
import com.organizen.app.home.data.*
import java.time.LocalDate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.zIndex
import java.time.Instant
import java.time.ZoneId


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(vm: AuthViewModel, tasksVm: TasksViewModel = viewModel()) {
    val userId = vm.currentUser?.uid ?: "guest"
    val tasks = tasksVm.tasksFor(userId)
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var showDatePicker = remember { mutableStateOf(false) }

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
                    dismissThresholds = {
                        FixedThreshold(100.dp)
                    },
                    background = {
                        if (dismissState.targetValue == DismissValue.DismissedToStart) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Red.copy(alpha = 0.3f))
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.CenterEnd
                            )
                            {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
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
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    if (showDialog) {
        TaskDialog(
            task = editingTask,
            onDismiss = { showDialog = false; editingTask = null },
            showDatePicker = showDatePicker,
            onSave = {
                tasksVm.upsertTask(userId, it)
                showDialog = false
                editingTask = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun TaskCard(task: Task, onCheckedChange: (Boolean) -> Unit, onClick: () -> Unit) {
    val overdue = LocalDate.now().isAfter(task.deadline) && !task.completed
    val dueToday = task.deadline == LocalDate.now() && !task.completed
    val difficultyColor = when (task.difficulty) {
        Difficulty.EASY -> Color.Green
        Difficulty.MEDIUM -> Color.Yellow
        Difficulty.HARD -> Color.Red
    }
    val categoryIcons = mapOf(
        Category.LIFESTYLE to Icons.Default.SelfImprovement,
        Category.LEARNING to Icons.Default.School,
        Category.SPORT to Icons.Default.FitnessCenter
    )
    val categoryColors = mapOf(
        Category.SPORT to Color(0xFFE3F2FD),
        Category.LIFESTYLE to Color(0xFFF1F8E9),
        Category.LEARNING to Color(0xFFFFF8E1)
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        border = if (overdue) BorderStroke(1.dp, Color.Red) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (task.completed) Color.LightGray else categoryColors[task.category] ?: MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp)
                .padding(8.dp)
        ) {
            if (overdue) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val spacing = 20.dp.toPx()
                    var x = -size.height
                    while (x < size.width) {
                        drawLine(
                            color = Color.Red,
                            start = Offset(x, 0f),
                            end = Offset(x + size.height, size.height),
                            strokeWidth = 2.dp.toPx()
                        )
                        x += spacing
                    }
                }
            }
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
                    Icon(
                        Icons.Default.Circle,
                        contentDescription = null,
                        tint = difficultyColor,
                        modifier = Modifier.size(12.dp)
                    )
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
                categoryIcons[task.category]?.let { icon ->
                    Icon(
                        icon,
                        contentDescription = task.category.name,
                        modifier = Modifier.size(24.dp)
                    )
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
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text("${task.estimatedMinutes}m", style = MaterialTheme.typography.bodySmall)
                if (dueToday) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.PriorityHigh,
                        contentDescription = "Due today",
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                }
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
private fun TaskDialog(
    task: Task?,
    onDismiss: () -> Unit,
    showDatePicker: MutableState<Boolean>,
    onSave: (Task) -> Unit
) {
    var description by remember { mutableStateOf(task?.description ?: "") }
    var estimated by remember { mutableStateOf(task?.estimatedMinutes?.toString() ?: "") }

    var difficulty by remember { mutableStateOf(task?.difficulty ?: Difficulty.EASY) }
    var selectedCategory by remember { mutableStateOf(task?.category ?: Category.LIFESTYLE) }
    var selectedDate by remember { mutableStateOf(task?.deadline) }

    AlertDialog(
        modifier = Modifier.zIndex(2f),
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
                            deadline = selectedDate ?: task?.deadline ?: LocalDate.now(),
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
                Box(
                    modifier = Modifier
//                        .padding(padding)
                        .fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedDate?.toString() ?: "",
                        onValueChange = { },
                        label = { Text("Deadline") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                showDatePicker.value = !showDatePicker.value
                            }) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Select date"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    )

                }
                Text("Difficulty")
                Row {
                    Difficulty.values().forEach { d ->
                        FilterChip(
                            selected = difficulty == d,
                            onClick = { difficulty = d },
                            label = {
                                Text(
                                    d.name.lowercase().replaceFirstChar { it.uppercase() })
                            }
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
                            label = {
                                Text(cat.name.lowercase().replaceFirstChar { it.uppercase() })
                            }
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                }
            }

        }
    )
    if (showDatePicker.value) {
        DatePickerModal(onDateSelected = {
            selectedDate = Instant.ofEpochMilli(it ?: 0L).atZone(ZoneId.systemDefault()).toLocalDate()
            showDatePicker.value = false
        }, onDismiss = {
            showDatePicker.value = false
        })
    }
}
