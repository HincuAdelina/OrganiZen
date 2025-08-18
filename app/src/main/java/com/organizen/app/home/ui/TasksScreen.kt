package com.organizen.app.home.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FractionalThreshold
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberSwipeableState
import androidx.compose.foundation.gestures.swipeable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.organizen.app.auth.AuthViewModel
import com.organizen.app.home.data.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
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
                val swipeState = rememberSwipeableState(0)
                val scope = rememberCoroutineScope()
                val deleteSize = 72.dp
                val deletePx = with(LocalDensity.current) { deleteSize.toPx() }
                Box(
                    Modifier
                        .padding(vertical = 4.dp)
                        .swipeable(
                            state = swipeState,
                            anchors = mapOf(0f to 0, -deletePx to 1),
                            thresholds = { _, _ -> FractionalThreshold(0.3f) },
                            orientation = Orientation.Horizontal
                        )
                ) {
                    Box(
                        Modifier
                            .matchParentSize(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        IconButton(onClick = {
                            tasksVm.removeTask(userId, task.id)
                            scope.launch { swipeState.animateTo(0) }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    TaskCard(
                        task = task,
                        onCheckedChange = { done -> tasksVm.setDone(userId, task.id, done) },
                        onClick = { editingTask = task; showDialog = true },
                        modifier = Modifier.offset { IntOffset(swipeState.offset.value.roundToInt(), 0) }
                    )
                }
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
private fun TaskCard(task: Task, onCheckedChange: (Boolean) -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val overdue = task.deadline.isBefore(LocalDate.now()) && !task.completed
    val dueToday = task.deadline.isEqual(LocalDate.now()) && !task.completed
    val difficultyColor = when (task.difficulty) {
        Difficulty.EASY -> Color.Green
        Difficulty.MEDIUM -> Color.Yellow
        Difficulty.HARD -> Color.Red
    }
    val categoryIcons = mapOf(
        Category.SPORT to Icons.Default.FitnessCenter,
        Category.DAILY to Icons.Default.Home,
        Category.LEARNING to Icons.Default.School
    )
    val categoryColors = mapOf(
        Category.SPORT to Color(0xFFD0F0FD),
        Category.DAILY to Color(0xFFFFF9C4),
        Category.LEARNING to Color(0xFFE8EAF6)
    )
    val hashedBrush = Brush.linearGradient(
        colors = listOf(Color.Red.copy(alpha = 0.3f), Color.Transparent),
        start = Offset.Zero,
        end = Offset(10f, 10f),
        tileMode = TileMode.Repeated
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                Box(
                    Modifier
                        .matchParentSize()
                        .background(hashedBrush)
                )
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
                    Icon(Icons.Default.Circle, contentDescription = null, tint = difficultyColor, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    if (dueToday) {
                        Icon(Icons.Default.PriorityHigh, contentDescription = "Due today", tint = Color.Red, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                    }
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
                        Icon(icon, contentDescription = task.category.name, modifier = Modifier.size(24.dp))
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
    var deadlineDate by remember { mutableStateOf(task?.deadline ?: LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var difficulty by remember { mutableStateOf(task?.difficulty ?: Difficulty.EASY) }
    var category by remember { mutableStateOf(task?.category ?: Category.DAILY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val minutes = estimated.toIntOrNull()
                val date = deadlineDate
                if (description.isNotBlank() && minutes != null) {
                    onSave(
                        Task(
                            id = task?.id ?: System.currentTimeMillis(),
                            description = description,
                            difficulty = difficulty,
                            estimatedMinutes = minutes,
                            category = category,
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
                OutlinedTextField(
                    value = deadlineDate.toString(),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.clickable { showDatePicker = true },
                    label = { Text("Deadline") }
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
                    Category.values().forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                }
            }
        }
    )

    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = deadlineDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = dateState.selectedDateMillis
                    if (millis != null) {
                        deadlineDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }
}
