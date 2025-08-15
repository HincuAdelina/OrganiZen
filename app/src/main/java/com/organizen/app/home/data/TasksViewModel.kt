package com.organizen.app.home.data

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel

class TasksViewModel : ViewModel() {
    private val tasksByUser = mutableStateMapOf<String, SnapshotStateList<Task>>()

    fun tasksFor(userId: String): SnapshotStateList<Task> =
        tasksByUser.getOrPut(userId) { mutableStateListOf() }

    fun upsertTask(userId: String, task: Task) {
        val tasks = tasksFor(userId)
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index >= 0) tasks[index] = task else tasks.add(task)
    }

    fun removeTask(userId: String, taskId: Long) {
        tasksFor(userId).removeAll { it.id == taskId }
    }

    fun setDone(userId: String, taskId: Long, done: Boolean) {
        val tasks = tasksFor(userId)
        val index = tasks.indexOfFirst { it.id == taskId }
        if (index >= 0) {
            val t = tasks[index]
            tasks[index] = t.copy(completed = done)
        }
    }
}
