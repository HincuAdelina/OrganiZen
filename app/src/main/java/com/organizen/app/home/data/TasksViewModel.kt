package com.organizen.app.home.data

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class TasksViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("tasks", Context.MODE_PRIVATE)
    private val tasksByUser = mutableStateMapOf<String, SnapshotStateList<Task>>()

    fun tasksFor(userId: String): SnapshotStateList<Task> =
        tasksByUser.getOrPut(userId) {
            mutableStateListOf<Task>().apply { addAll(loadTasks(userId)) }
        }

    fun upsertTask(userId: String, task: Task) {
        val tasks = tasksFor(userId)
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index >= 0) tasks[index] = task else tasks.add(task)
        saveTasks(userId)
    }

    fun removeTask(userId: String, taskId: Long) {
        tasksFor(userId).removeAll { it.id == taskId }
        saveTasks(userId)
    }

    fun setDone(userId: String, taskId: Long, done: Boolean) {
        val tasks = tasksFor(userId)
        val index = tasks.indexOfFirst { it.id == taskId }
        if (index >= 0) {
            val t = tasks[index]
            tasks[index] = t.copy(completed = done)
            saveTasks(userId)
        }
    }

    private fun loadTasks(userId: String): List<Task> {
        val json = prefs.getString(userId, null) ?: return emptyList()
        val arr = JSONArray(json)
        val list = mutableListOf<Task>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val category = Category.valueOf(o.getString("category"))
            list += Task(
                id = o.getLong("id"),
                description = o.getString("description"),
                difficulty = Difficulty.valueOf(o.getString("difficulty")),
                estimatedMinutes = o.getInt("estimatedMinutes"),
                category = category,
                deadline = LocalDate.parse(o.getString("deadline")),
                completed = o.getBoolean("completed")
            )
        }
        return list
    }

    private fun saveTasks(userId: String) {
        val arr = JSONArray()
        tasksByUser[userId]?.forEach { task ->
            val o = JSONObject()
            o.put("id", task.id)
            o.put("description", task.description)
            o.put("difficulty", task.difficulty.name)
            o.put("estimatedMinutes", task.estimatedMinutes)
            o.put("category", task.category.name)
            o.put("deadline", task.deadline.toString())
            o.put("completed", task.completed)
            arr.put(o)
        }
        prefs.edit().putString(userId, arr.toString()).apply()
    }
}

