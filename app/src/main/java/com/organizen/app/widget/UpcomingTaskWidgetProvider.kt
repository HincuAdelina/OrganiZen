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
import com.organizen.app.home.data.Task
import com.organizen.app.home.data.Category
import com.organizen.app.home.data.Difficulty
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class UpcomingTaskWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateAppWidget(context, appWidgetManager, it) }
    }

    // Prinde refresh-urile trimise din aplicație
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> updateAll(context)
        }
    }

    // Actualizeaza toate instanțele widgetului
    private fun updateAll(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, UpcomingTaskWidgetProvider::class.java))
        ids.forEach { updateAppWidget(context, mgr, it) }
    }

    companion object {
        // Acțiune custom pt. refresh
        const val ACTION_REFRESH = "com.organizen.app.widget.REFRESH"

        // Apeleaza din app după ce salvezi/editezi taskuri
        fun requestRefresh(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, UpcomingTaskWidgetProvider::class.java))
            val i = Intent(context, UpcomingTaskWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(i)
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.upcoming_task_widget)

            // tap pe card -> deschide aplicația
            val intent = Intent(context, MainActivity::class.java)
            val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, pending)

            val task = loadTasks(context)
                .filter { !it.completed && !it.deadline.isBefore(LocalDate.now()) }
                .minByOrNull { it.deadline }

            if (task != null) {
                val categoryLabel = task.category.name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) }
                val diffLabel = task.difficulty.name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) }

                views.setTextViewText(R.id.widget_task_description, task.description)

                // chip-uri
                views.setTextViewText(R.id.widget_task_category, categoryLabel)
                views.setTextViewText(R.id.widget_task_time, context.getString(R.string.minutes_short, task.estimatedMinutes))

                // deadline prietenos
                val due = formatDue(context, task.deadline)
                views.setTextViewText(R.id.widget_task_deadline, context.getString(R.string.due_prefix, due))

                // bulina dificultate + text
                val bullet = "\u25CF"
                views.setTextViewText(R.id.widget_task_difficulty, "$bullet $diffLabel")
                views.setTextColor(R.id.widget_task_difficulty, colorFor(task.difficulty))

                views.setViewVisibility(R.id.widget_info_row, View.VISIBLE)
                views.setViewVisibility(R.id.widget_deadline_row, View.VISIBLE)
            } else {
                val msg = context.getString(R.string.no_upcoming_tasks)
                views.setTextViewText(R.id.widget_task_description, msg)
                views.setTextViewText(R.id.widget_task_category, "")
                views.setTextViewText(R.id.widget_task_time, "")
                views.setTextViewText(R.id.widget_task_deadline, "")
                views.setTextViewText(R.id.widget_task_difficulty, "")
                views.setViewVisibility(R.id.widget_info_row, View.GONE)
                views.setViewVisibility(R.id.widget_deadline_row, View.GONE)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun formatDue(context: Context, date: LocalDate): String {
            val today = LocalDate.now()
            return when (date) {
                today -> context.getString(R.string.today)
                today.plusDays(1) -> context.getString(R.string.tomorrow)
                in today.plusDays(2)..today.plusDays(6) ->
                    date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                else -> date.toString()
            }
        }

        private fun colorFor(diff: Difficulty): Int = when (diff) {
            Difficulty.EASY -> Color.parseColor("#4CAF50")
            Difficulty.MEDIUM -> Color.parseColor("#FFC107")
            Difficulty.HARD -> Color.parseColor("#F44336")
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
