package com.organizen.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.organizen.app.MainActivity
import com.organizen.app.R
import com.organizen.app.home.data.Category
import com.organizen.app.home.data.Difficulty
import com.organizen.app.home.data.Task
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class UpcomingTaskWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_COMPLETE) {
            val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
            if (taskId != -1L) {
                val tasks = loadTasks(context).toMutableList()
                val idx = tasks.indexOfFirst { it.id == taskId }
                if (idx != -1) {
                    tasks[idx] = tasks[idx].copy(completed = true)
                    saveTasks(context, tasks)
                }
            }
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, UpcomingTaskWidgetProvider::class.java))
            ids.forEach { updateAppWidget(context, manager, it) }
        }
    }

    companion object {
        private const val ACTION_COMPLETE = "com.organizen.app.widget.COMPLETE_TASK"
        private const val EXTRA_TASK_ID = "task_id"

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.upcoming_task_widget)
            val task = loadTasks(context).filter { !it.completed && !it.deadline.isBefore(LocalDate.now()) }
                .minByOrNull { it.deadline }

            val launchIntent = Intent(context, MainActivity::class.java)
            val launchPending = PendingIntent.getActivity(
                context, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, launchPending)

            if (task != null) {
                views.setViewVisibility(R.id.widget_task_checkbox, View.VISIBLE)
                views.setViewVisibility(R.id.widget_info_container, View.VISIBLE)
                views.setTextViewText(R.id.widget_task_description, task.description)
                views.setTextViewText(
                    R.id.widget_task_category,
                    task.category.name.lowercase().replaceFirstChar { it.uppercase() }
                )
                views.setTextViewText(
                    R.id.widget_task_estimated,
                    context.getString(R.string.minutes_short, task.estimatedMinutes)
                )
                views.setTextViewText(
                    R.id.widget_task_deadline,
                    context.getString(R.string.due_prefix, task.deadline.toString())
                )
                views.setTextViewText(R.id.widget_task_difficulty, "\u25CF")
                val color = when (task.difficulty) {
                    Difficulty.EASY -> Color.GREEN
                    Difficulty.MEDIUM -> Color.YELLOW
                    Difficulty.HARD -> Color.RED
                }
                views.setTextColor(R.id.widget_task_difficulty, color)
                views.setBoolean(R.id.widget_task_checkbox, "setChecked", false)

                val checkIntent = Intent(context, UpcomingTaskWidgetProvider::class.java).apply {
                    action = ACTION_COMPLETE
                    putExtra(EXTRA_TASK_ID, task.id)
                }
                val checkPending = PendingIntent.getBroadcast(
                    context,
                    task.id.toInt(),
                    checkIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                views.setOnClickPendingIntent(R.id.widget_task_checkbox, checkPending)
            } else {
                views.setViewVisibility(R.id.widget_task_checkbox, View.GONE)
                views.setViewVisibility(R.id.widget_info_container, View.GONE)
                views.setTextViewText(
                    R.id.widget_task_description,
                    context.getString(R.string.no_upcoming_tasks)
                )
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun loadTasks(context: Context): List<Task> {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
            val prefs = context.getSharedPreferences("tasks", Context.MODE_PRIVATE)
            val json = prefs.getString(userId, null) ?: return emptyList()
            val arr = JSONArray(json)
            val list = mutableListOf<Task>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val category = if (o.has("category")) {
                    Category.valueOf(o.getString("category"))
                } else {
                    val tagsArr = o.getJSONArray("tags")
                    if (tagsArr.length() > 0) Category.valueOf(tagsArr.getString(0)) else Category.LIFESTYLE
                }
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

        private fun saveTasks(context: Context, tasks: List<Task>) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
            val arr = JSONArray()
            tasks.forEach { t ->
                val o = JSONObject()
                o.put("id", t.id)
                o.put("description", t.description)
                o.put("difficulty", t.difficulty.name)
                o.put("estimatedMinutes", t.estimatedMinutes)
                o.put("category", t.category.name)
                o.put("deadline", t.deadline.toString())
                o.put("completed", t.completed)
                arr.put(o)
            }
            val prefs = context.getSharedPreferences("tasks", Context.MODE_PRIVATE)
            prefs.edit().putString(userId, arr.toString()).apply()
        }
    }
}
