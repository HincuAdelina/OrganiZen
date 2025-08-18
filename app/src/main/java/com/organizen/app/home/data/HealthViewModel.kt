package com.organizen.app.home.data

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class HealthViewModel(application: Application) : AndroidViewModel(application) {
    private val client = HealthConnectClient.getOrCreate(application)

    val steps = mutableStateOf<Long?>(null)
    val sleepHours = mutableStateOf<Double?>(null)

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class)
    )

    fun loadHealthData() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val zone = ZoneId.systemDefault()
            val start = today.atStartOfDay(zone).toInstant()
            val end = today.plusDays(1).atStartOfDay(zone).toInstant()

            val stepsResult = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            steps.value = stepsResult.records.sumOf { it.count }

            val sleepResult = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            val totalSleepMillis = sleepResult.records.sumOf {
                it.endTime.toEpochMilli() - it.startTime.toEpochMilli()
            }
            sleepHours.value = totalSleepMillis / (1000.0 * 60 * 60)
        }
    }
}

