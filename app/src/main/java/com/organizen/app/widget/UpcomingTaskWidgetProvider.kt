package com.organizen.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.organizen.app.R
import com.organizen.app.home.data.Task
import com.organizen.app.home.data.Category
import com.organizen.app.home.data.Difficulty
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import java.time.LocalDate

class UpcomingTaskWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.upcoming_task_widget)
            val task = loadTasks(context)
                .filter { !it.completed && !it.deadline.isBefore(LocalDate.now()) }
                .minByOrNull { it.deadline }
            if (task != null) {
                val category = task.category.name.lowercase().replaceFirstChar { it.uppercase() }
                val difficulty = task.difficulty.name.lowercase().replaceFirstChar { it.uppercase() }
                views.setTextViewText(R.id.widget_task_description, task.description)
                views.setTextViewText(R.id.widget_task_category, "Category: $category")
                views.setTextViewText(R.id.widget_task_difficulty, "Difficulty: $difficulty")
                views.setTextViewText(R.id.widget_task_time, "Time: ${task.estimatedMinutes} min")
                views.setTextViewText(R.id.widget_task_deadline, "Deadline: ${task.deadline}")
            } else {
                val msg = context.getString(R.string.no_upcoming_tasks)
                views.setTextViewText(R.id.widget_task_description, msg)
                views.setTextViewText(R.id.widget_task_category, "")
                views.setTextViewText(R.id.widget_task_difficulty, "")
                views.setTextViewText(R.id.widget_task_time, "")
                views.setTextViewText(R.id.widget_task_deadline, "")
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
    }
}
