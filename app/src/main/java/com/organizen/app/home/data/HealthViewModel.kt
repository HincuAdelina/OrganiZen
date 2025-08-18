package com.organizen.app.home.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HealthViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = HealthRepository(application)

    var steps by mutableStateOf<Long?>(null)
        private set
    var sleepHours by mutableStateOf<Double?>(null)
        private set

    init {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val start = LocalDate.now().atStartOfDay(zone).toInstant()
            val end = start.plus(1, ChronoUnit.DAYS)

            val stepRecords = repo.readStepsInputs(start, end)
            steps = stepRecords.sumOf { it.count.toLong() }

            val sleepRecords = repo.readSleepInputs(start, end)
            val totalSleepSeconds = sleepRecords.sumOf {
                Duration.between(it.startTime, it.endTime).seconds
            }
            sleepHours = totalSleepSeconds / 3600.0
        }
    }
}
