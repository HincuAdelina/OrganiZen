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
import androidx.compose.ui.graphics.drawscope.rotate
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
import kotlin.math.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProductivitySection(
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
    val tired = vm.sleepHours!! < vm.sleepGoal || vm.steps!! > vm.stepsGoal
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
    // în ProductivitySection, după calculul sleepHoursPart/MinutesPart, adaugă:
    val sleepGoalTotalMin = (vm.sleepGoal * 60).roundToInt()
    val sleepGoalH = sleepGoalTotalMin / 60
    val sleepGoalM = sleepGoalTotalMin % 60

    // progress (0..1)
    val stepsProgress = (vm.steps!!.toFloat() / vm.stepsGoal).coerceIn(0f, 1f)
    val sleepProgress = (vm.sleepHours!! / vm.sleepGoal).toFloat().coerceIn(0f, 1f)

    val scroll = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll) // scroll pe toată pagina
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ——— două inele pe același rând (fără Card în spate) ———
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
                centerBottom = "of ${vm.stepsGoal.toInt()}",
                progress = stepsProgress,
                ringSize = 130.dp,
                ringStroke = 12.dp,
                startColor = MaterialTheme.colorScheme.primary,
                endColor = MaterialTheme.colorScheme.tertiary
            )
            StatRingTile(
                modifier = Modifier.weight(1f),
                title = "Sleep",
                icon = Icons.Filled.Bedtime,
                centerTop = "${sleepHoursPart}h ${sleepMinutesPart}m",
                centerBottom = "goal ${sleepGoalH}h ${sleepGoalM}m", // <- acum arată și minutele
                progress = sleepProgress,
                ringSize = 130.dp,
                ringStroke = 12.dp,
                startColor = MaterialTheme.colorScheme.primary,
                endColor = MaterialTheme.colorScheme.secondary
            )
        }

        // ——— Recomandare + buton ———
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

/** Tile fără Card: titlu + inel cu gradient (start→end) și capete glow. */
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
    startColor: Color,
    endColor: Color
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
            startColor = startColor,
            endColor = endColor
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

/**
 * Inel de progres cu:
 *  - gradient „liniar” de la start→end de-a lungul arcului (folosind sweepGradient cu stops limitate la sweep)
 *  - halo subtil pe tot arc-ul
 *  - glow la capete (radial)
 */
@Composable
private fun GoalRing(
    progress: Float,
    modifier: Modifier = Modifier,
    ringSize: Dp = 130.dp,
    stroke: Dp = 12.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    startColor: Color = Color(0xFF5B86E5),
    endColor: Color = Color(0xFF36D1DC),
    startAngle: Float = -90f, // de sus
    content: @Composable BoxScope.() -> Unit
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(900),
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
            val center = Offset(size.width / 2f, size.height / 2f)
            val topLeft = Offset(center.x - arcSize.width / 2f, center.y - arcSize.height / 2f)

            // TRACK (fundal)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Gradient „linear pe arc” (folosim sweep cu stops limitate la segment)
            val sweep = 360f * animated
            val frac = max(0.001f, (sweep / 360f).coerceIn(0f, 1f))
            val stops = arrayOf(
                0f to startColor,
                frac to endColor,
                1f to endColor
            )

            // ARC principal (fără halo/glow)
            rotate(degrees = startAngle, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(colorStops = stops, center = center),
                    startAngle = 0f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        // centru (număr + sublabel)
        content()
    }
}
