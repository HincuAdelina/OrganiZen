package com.organizen.app.home.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.organizen.app.auth.AuthViewModel
import com.organizen.app.home.data.ChatViewModel
import com.organizen.app.home.data.HealthViewModel
import com.organizen.app.home.data.TasksViewModel
import com.organizen.app.navigation.BottomNavScreen
import java.time.LocalDate
import kotlin.math.min
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HealthSection(
    authVm: AuthViewModel,
    navController: NavController,
    chatViewModel: ChatViewModel,
    tasksVm: TasksViewModel,
    vm: HealthViewModel = viewModel(),
) {
    if (vm.steps == null || vm.sleepHours == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val userId = authVm.currentUser?.uid ?: "guest"
    val tasks = tasksVm.tasksFor(userId)
    val available = tasks.filter { !it.completed && !it.deadline.isBefore(LocalDate.now()) }

    // Consider the user tired only when today's sleep is low or they have already walked a lot
    val tired = vm.sleepHours!! < 7.0 || vm.steps!! > 5000
    val recommended = if (available.isNotEmpty()) {
        val easiest = available.minByOrNull { it.difficulty.ordinal }
        val shortest = available.minByOrNull { it.estimatedMinutes }
        val hardest = available.maxByOrNull { it.difficulty.ordinal }
        val longest = available.maxByOrNull { it.estimatedMinutes }
        val candidates = if (tired) {
            listOfNotNull(easiest, shortest)
        } else {
            listOfNotNull(hardest, longest)
        }.distinct()
        if (candidates.isNotEmpty()) candidates.random() else null
    } else null

    // sleep format
    val sleepMinutes = (vm.sleepHours!! * 60).roundToInt()
    val sleepHoursPart = sleepMinutes / 60
    val sleepMinutesPart = sleepMinutes % 60

    // goals
    val stepsGoal = 5000f
    val sleepGoal = 7.0

    // progress (0..1)
    val stepsProgress = (vm.steps!! / stepsGoal).coerceIn(0f, 1f)
    val sleepProgress = (vm.sleepHours!! / sleepGoal).toFloat().coerceIn(0f, 1f)

    val scroll = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll) // <<— SCROLL pe toată pagina
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ——— Rând cu cele două inele (fără Card) ———
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatRingTile(
                modifier = Modifier.weight(1f),
                title = "Steps",
                icon = Icons.Filled.DirectionsWalk,
                centerTop = "%,d".format(vm.steps),
                centerBottom = "of ${stepsGoal.toInt()}",
                progress = stepsProgress,
                ringSize = 140.dp,      // mai mic să încapă două pe un rând
                ringStroke = 12.dp,
                gradientColors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary
                )
            )
            StatRingTile(
                modifier = Modifier.weight(1f),
                title = "Sleep",
                icon = Icons.Filled.Bedtime,
                centerTop = "${sleepHoursPart}h ${sleepMinutesPart}m",
                centerBottom = "goal ${sleepGoal.toInt()}h",
                progress = sleepProgress,
                ringSize = 140.dp,
                ringStroke = 12.dp,
                gradientColors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary
                )
            )
        }

        // ——— Recomandarea + buton (vor face scroll când nu încape) ———
        recommended?.let { task ->
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Recommended task", style = MaterialTheme.typography.titleMedium)
                TaskCard(
                    task = task,
                    onCheckedChange = { done -> tasksVm.setDone(userId, task.id, done) },
                    onClick = {},
                )
            }
        }

        Button(
            onClick = {
                val prompt = if (tired) "Suggest a short relaxation exercise."
                else "Give me a productivity advice to stay focused."
                chatViewModel.sendMessage(prompt)
                navController.navigate(BottomNavScreen.Chat.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (tired) "Get relax recommendations" else "Get productivity advice")
        }
    }
}

/** Variantă fără Card; doar titlu + inelul cu gradient și text central. */
@Composable
private fun StatRingTile(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    centerTop: String,
    centerBottom: String,
    progress: Float,
    ringSize: Dp,
    ringStroke: Dp,
    gradientColors: List<Color>
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        GoalRing(
            progress = progress,
            ringSize = ringSize,
            stroke = ringStroke,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            gradientColors = gradientColors
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    centerTop,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    centerBottom,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun GoalRing(
    progress: Float,
    modifier: Modifier = Modifier,
    ringSize: Dp = 140.dp,
    stroke: Dp = 12.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    gradientColors: List<Color> = listOf(Color(0xFF5B86E5), Color(0xFF36D1DC)),
    startAngle: Float = -90f, // start from top
    content: @Composable BoxScope.() -> Unit
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(800),
        label = "ringProgress"
    )

    Box(
        modifier = modifier.size(ringSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val diameter = min(size.width, size.height)
            val arcSize = Size(diameter - strokePx, diameter - strokePx)
            val topLeft = Offset(
                (size.width - arcSize.width) / 2f,
                (size.height - arcSize.height) / 2f
            )

            // fundalul inelului
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // progresul cu gradient
            val brush = Brush.sweepGradient(
                colors = gradientColors,
                center = Offset(size.width / 2f, size.height / 2f)
            )

            drawArc(
                brush = brush,
                startAngle = startAngle,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        content()
    }
}
