package com.organizen.app.home.ui

import android.app.usage.UsageStatsManager
import android.os.Build
import android.content.Context
import androidx.compose.ui.platform.LocalContext
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
import androidx.navigation.NavController
import com.organizen.app.auth.AuthViewModel
import com.organizen.app.home.data.ChatViewModel
import com.organizen.app.home.data.HealthViewModel
import com.organizen.app.home.data.TasksViewModel
import com.organizen.app.navigation.BottomNavScreen
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.*
import android.app.usage.UsageEvents
import android.content.pm.PackageManager
import android.content.Intent
import androidx.compose.ui.geometry.CornerRadius


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProductivitySection(
    authVm: AuthViewModel,
    navController: NavController,
    chatViewModel: ChatViewModel,
    tasksVm: TasksViewModel,
    vm: HealthViewModel,
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

    // zona EEST
    val zone = java.time.ZoneId.of("Europe/Bucharest")

// cheie zilnică – se schimbă la miezul nopții local
    var todayKey by remember { mutableStateOf(java.time.LocalDate.now(zone)) }

// reprogramează refresh-ul exact la 00:00 local
    LaunchedEffect(todayKey) {
        val now = java.time.ZonedDateTime.now(zone)
        val nextMidnight = todayKey.plusDays(1).atStartOfDay(zone)
        val delayMs = java.time.Duration.between(now, nextMidnight).toMillis().coerceAtLeast(0)
        kotlinx.coroutines.delay(delayMs)
        todayKey = java.time.LocalDate.now(zone)
    }

    val scroll = rememberScrollState()
    val context = LocalContext.current

// ⬇️ înlocuiește blocul tău cu queryUsageStats cu acesta:
    val topUsage = remember(todayKey, context) {
        top3LaunchableAppsSinceMidnightUsingEvents(context, zone)
    }
    val maxUsageMinutes = topUsage.maxOfOrNull { it.second } ?: 0

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

        if (topUsage.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Most used apps today", style = MaterialTheme.typography.titleMedium)
                    topUsage.forEach { (name, minutes) ->
                        val progress = if (maxUsageMinutes > 0) minutes / maxUsageMinutes.toFloat() else 0f
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(name, modifier = Modifier.width(100.dp))
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.weight(1f).height(20.dp)) {
                                PillLinearProgress(
                                    progress = progress,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(end = 48.dp) // loc pentru eticheta de minute (ca să nu se suprapună)
                                )
                                Text(
                                    formatMinutes(minutes),
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 4.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }


                        }
                    }
                }
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

private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
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



private fun top3LaunchableAppsSinceMidnightUsingEvents(
    context: Context,
    zone: ZoneId
): List<Pair<String, Int>> {
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val pm = context.packageManager

    val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
    val end = System.currentTimeMillis()

    // 1) Pachete „launchable” (MAIN + LAUNCHER)
    val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION") pm.queryIntentActivities(launcherIntent, 0)
    }
    val launchablePkgs = resolved.map { it.activityInfo.packageName }.toSet()

    // 2) Excluderi (fără propria aplicație!)
    val exclude = setOf(
        "com.android.systemui",
        "com.google.android.apps.nexuslauncher",
        "com.android.launcher",
        "com.huawei.android.launcher",
        "com.miui.home",
        "com.sec.android.app.launcher",
        "com.oneplus.launcher",
        "com.oppo.launcher",
        "com.vivo.launcher"
    )

    // 3) Calculează foreground time din evenimente
    val resumedTypes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        setOf(UsageEvents.Event.ACTIVITY_RESUMED) else setOf(UsageEvents.Event.MOVE_TO_FOREGROUND)
    val pausedTypes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        setOf(UsageEvents.Event.ACTIVITY_PAUSED) else setOf(UsageEvents.Event.MOVE_TO_BACKGROUND)

    val openSince = mutableMapOf<String, Long>()
    val totals = mutableMapOf<String, Long>()
    val events = usm.queryEvents(start, end)
    val e = UsageEvents.Event()

    while (events.hasNextEvent()) {
        events.getNextEvent(e)
        val pkg = e.packageName ?: continue
        val t = e.timeStamp
        when (e.eventType) {
            in resumedTypes -> openSince[pkg] = t
            in pausedTypes -> openSince.remove(pkg)?.let { t0 ->
                if (t >= t0) totals[pkg] = (totals[pkg] ?: 0L) + (t - t0)
            }
        }
    }
    // sesiuni încă deschise până la „end”
    openSince.forEach { (pkg, t0) ->
        totals[pkg] = (totals[pkg] ?: 0L) + (end - t0)
    }

    // 4)  Ținem DOAR aplicații launchable **SAU** propria aplicație; și scoatem excluderile
    val myPkg = context.packageName
    val filtered = totals.asSequence()
        .filter { (pkg, ms) ->
            ms > 0 && (pkg in launchablePkgs || pkg == myPkg) && pkg !in exclude
        }

    // 5) Map la (label, minute) și top 3
    return filtered
        .map { (pkg, ms) -> resolveAppLabel(pm, pkg) to (ms / 60_000L).toInt() }
        .sortedByDescending { it.second }
        .take(3)
        .toList()
}


private fun resolveAppLabel(pm: PackageManager, pkg: String): String = try {
    val ai = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        pm.getApplicationInfo(pkg, 0)
    }
    pm.getApplicationLabel(ai).toString()
} catch (_: Exception) {
    pkg
}


@Composable
private fun PillLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Canvas(modifier) {
        val p = progress.coerceIn(0f, 1f)
        val radius = size.height / 2f

        // track
        drawRoundRect(
            color = trackColor,
            cornerRadius = CornerRadius(radius, radius)
        )

        // fill cu capete rotunjite (pill) la nivelul progresului
        if (p > 0f) {
            drawRoundRect(
                color = color,
                size = androidx.compose.ui.geometry.Size(width = size.width * p, height = size.height),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
    }
}