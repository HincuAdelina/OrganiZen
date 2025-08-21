package com.organizen.app.home.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalContext
import com.organizen.app.widget.UpcomingTaskWidgetProvider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import java.time.Instant
import java.time.ZoneId

private enum class SortOption {
    DEFAULT,
    DIFFICULTY_ASC,
    DIFFICULTY_DESC,
    TIME_ASC,
    TIME_DESC,
    DEADLINE_ASC,
    DEADLINE_DESC
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(vm: AuthViewModel, tasksVm: TasksViewModel = viewModel()) {
    val userId = vm.currentUser?.uid ?: "guest"
    val tasks = tasksVm.tasksFor(userId)
    var editingTask by remember { mutableStateOf<Task?>(null) }
    val context = LocalContext.current.applicationContext
    var showDialog by remember { mutableStateOf(false) }
    var showDatePicker = remember { mutableStateOf(false) }

    var sortOption by remember { mutableStateOf(SortOption.DEFAULT) }
    var categoryFilter by remember { mutableStateOf<Category?>(null) }
    var difficultyFilter by remember { mutableStateOf<Difficulty?>(null) }

    val displayTasks by remember(tasks, sortOption, categoryFilter, difficultyFilter) {
        derivedStateOf {
            var list = tasks.toList()
            categoryFilter?.let { cat -> list = list.filter { it.category == cat } }
            difficultyFilter?.let { diff -> list = list.filter { it.difficulty == diff } }
            list = when (sortOption) {
                SortOption.DIFFICULTY_ASC -> list.sortedBy { it.difficulty.ordinal }
                SortOption.DIFFICULTY_DESC -> list.sortedByDescending { it.difficulty.ordinal }
                SortOption.TIME_ASC -> list.sortedBy { it.estimatedMinutes }
                SortOption.TIME_DESC -> list.sortedByDescending { it.estimatedMinutes }
                SortOption.DEADLINE_ASC -> list.sortedBy { it.deadline }
                SortOption.DEADLINE_DESC -> list.sortedByDescending { it.deadline }
                SortOption.DEFAULT -> list
            }
            val (pending, done) = list.partition { !it.completed }
            pending + done
        }
    }
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var sortExpanded by remember { mutableStateOf(false) }
                Box {
                    Button(onClick = { sortExpanded = true }) { Text("Sort") }
                    DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }) {
                        DropdownMenuItem(text = { Text("Default") }, onClick = {
                            sortOption = SortOption.DEFAULT
                            sortExpanded = false
                        })
                        DropdownMenuItem(text = { Text("Easy → Hard") }, onClick = {
                            sortOption = SortOption.DIFFICULTY_ASC
                            sortExpanded = false
                        })
                        DropdownMenuItem(text = { Text("Hard → Easy") }, onClick = {
                            sortOption = SortOption.DIFFICULTY_DESC
                            sortExpanded = false
                        })
                        DropdownMenuItem(text = { Text("Shortest time") }, onClick = {
                            sortOption = SortOption.TIME_ASC
                            sortExpanded = false
                        })
                        DropdownMenuItem(text = { Text("Longest time") }, onClick = {
                            sortOption = SortOption.TIME_DESC
                            sortExpanded = false
                        })
                        DropdownMenuItem(text = { Text("Nearest deadline") }, onClick = {
                            sortOption = SortOption.DEADLINE_ASC
                            sortExpanded = false
                        })
                        DropdownMenuItem(text = { Text("Farthest deadline") }, onClick = {
                            sortOption = SortOption.DEADLINE_DESC
                            sortExpanded = false
                        })
                    }
                }

                var catExpanded by remember { mutableStateOf(false) }
                Box {
                    Button(onClick = { catExpanded = true }) {
                        Text(categoryFilter?.name ?: "Category")
                    }
                    DropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }) {
                        DropdownMenuItem(text = { Text("All") }, onClick = {
                            categoryFilter = null
                            catExpanded = false
                        })
                        Category.values().forEach { cat ->
                            DropdownMenuItem(text = {
                                Text(cat.name.lowercase().replaceFirstChar { it.uppercase() })
                            }, onClick = {
                                categoryFilter = cat
                                catExpanded = false
                            })
                        }
                    }
                }

                var diffExpanded by remember { mutableStateOf(false) }
                Box {
                    Button(onClick = { diffExpanded = true }) {
                        Text(difficultyFilter?.name ?: "Difficulty")
                    }
                    DropdownMenu(
                        expanded = diffExpanded,
                        onDismissRequest = { diffExpanded = false }) {
                        DropdownMenuItem(text = { Text("All") }, onClick = {
                            difficultyFilter = null
                            diffExpanded = false
                        })
                        Difficulty.values().forEach { diff ->
                            DropdownMenuItem(text = {
                                Text(diff.name.lowercase().replaceFirstChar { it.uppercase() })
                            }, onClick = {
                                difficultyFilter = diff
                                diffExpanded = false
                            })
                        }
                    }
                }

//                if (sortOption != SortOption.DEFAULT || categoryFilter != null || difficultyFilter != null) {
//                    TextButton(onClick = {
//                        sortOption = SortOption.DEFAULT
//                        categoryFilter = null
//                        difficultyFilter = null
//                    }) {
//                        Text("Reset")
//                    }
//                }
            }

            Spacer(Modifier.height(8.dp))

            val listState = rememberLazyListState()
            LaunchedEffect(sortOption) { listState.scrollToItem(0) }
            LazyColumn(state = listState) {
                items(displayTasks, key = { it.id }) { task ->
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
                            val cardShape = CardDefaults.shape
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(cardShape)
                                    .background(
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                        shape = cardShape
                                    ),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                    },
                    dismissContent = {
                        TaskCard(
                            task = task,
                            onCheckedChange = { done ->
                                tasksVm.setDone(userId, task.id, done)
                                UpcomingTaskWidgetProvider.requestRefresh(context) // <— actualizează widgetul
                            },
                            onClick = { editingTask = task; showDialog = true }
                        )
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }}
    if (showDialog) {
        TaskDialog(
            task = editingTask,
            onDismiss = { showDialog = false; editingTask = null },
            showDatePicker = showDatePicker,
            onSave = {
                tasksVm.upsertTask(userId, it)
                UpcomingTaskWidgetProvider.requestRefresh(context)
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
fun TaskCard(task: Task, onCheckedChange: (Boolean) -> Unit, onClick: () -> Unit) {
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
            Checkbox(
                checked = task.completed,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            categoryIcons[task.category]?.let { icon ->
                Icon(
                    icon,
                    contentDescription = task.category.name,
                    modifier = Modifier.size(16.dp).align(Alignment.BottomStart)
                )
            }
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

            }
            Text(
                task.category.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
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
            }
            Text(
                if (dueToday) "Today" else task.deadline.toString(),
                color = if (dueToday) Color.Red else Color.Unspecified,
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
//                        .padding(8.dp)
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
