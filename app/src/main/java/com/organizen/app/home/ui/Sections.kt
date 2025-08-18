package com.organizen.app.home.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import com.organizen.app.home.data.HealthViewModel

@Composable
fun HealthScreen(vm: HealthViewModel = viewModel()) {
    val context = LocalContext.current
    val client = remember { HealthConnectClient.getOrCreate(context) }
    val permissions = remember {
        setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class)
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = client.permissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(permissions)) {
            vm.loadHealthData()
        }
    }

    LaunchedEffect(Unit) {
        val granted = client.permissionController.getGrantedPermissions(permissions)
        if (!granted.containsAll(permissions)) {
            launcher.launch(permissions)
        } else {
            vm.loadHealthData()
        }
    }

    val steps by vm.steps
    val sleepHours by vm.sleepHours

    Box(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (steps != null && sleepHours != null) {
            Text("Steps: $steps\nSleep: ${String.format("%.1f", sleepHours)} h")
        } else {
            Text("Loading health data...")
        }
    }
}

@Composable
fun ChatScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Chat section")
    }
}
