package com.organizen.app.widget

import android.content.Context
import android.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.update
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontSize
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.dp
import androidx.glance.unit.sp
import com.organizen.app.MainActivity
import com.organizen.app.R
import com.organizen.app.home.data.Category
import com.organizen.app.home.data.Difficulty
import com.organizen.app.home.data.Task
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class UpcomingTaskWidget : GlanceAppWidget() {
    override suspend fun Content() {
        val context = LocalContext.current
        val tasks = loadTasks(context).filter { !it.completed && !it.deadline.isBefore(LocalDate.now()) }
        val task = tasks.minByOrNull { it.deadline }

        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(R.drawable.widget_card_bg)
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            if (task != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CheckBox(
                        checked = false,
                        onCheckedChange = actionRunCallback<CompleteTaskAction>(
                            actionParametersOf(CompleteTaskAction.TASK_ID_KEY to task.id)
                        )
                    )
                    Text(
                        text = "\u25CF",
                        style = TextStyle(color = ColorProvider(colorFor(task.difficulty))),
                        modifier = GlanceModifier.padding(start = 4.dp)
                    )
                    Text(
                        text = task.description,
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        modifier = GlanceModifier.padding(start = 4.dp)
                    )
                }
                Row(modifier = GlanceModifier.padding(top = 4.dp)) {
                    Text(text = capitalize(task.category.name.lowercase()))
                    Text(
                        text = context.getString(R.string.minutes_short, task.estimatedMinutes),
                        modifier = GlanceModifier.padding(start = 8.dp)
                    )
                    Text(
                        text = context.getString(R.string.due_prefix, task.deadline.toString()),
                        modifier = GlanceModifier.padding(start = 8.dp)
                    )
                }
            } else {
                Text(text = context.getString(R.string.no_upcoming_tasks))
            }
        }
    }
}

class UpcomingTaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UpcomingTaskWidget()
}

class CompleteTaskAction : androidx.glance.action.ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TASK_ID_KEY] ?: return
        val tasks = loadTasks(context).toMutableList()
        val idx = tasks.indexOfFirst { it.id == taskId }
        if (idx != -1) {
            tasks[idx] = tasks[idx].copy(completed = true)
            saveTasks(context, tasks)
        }
        UpcomingTaskWidget().update(context, glanceId)
    }

    companion object {
        val TASK_ID_KEY = ActionParameters.Key<Long>("taskId")
    }
}

private fun colorFor(diff: Difficulty): Int = when (diff) {
    Difficulty.EASY -> Color.GREEN
    Difficulty.MEDIUM -> Color.YELLOW
    Difficulty.HARD -> Color.RED
}

private fun capitalize(text: String): String =
    text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

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
