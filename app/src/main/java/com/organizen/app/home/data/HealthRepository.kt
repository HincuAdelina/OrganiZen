package com.organizen.app.home.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant

class HealthRepository(private val ctx: Context) {
    private val client by lazy { HealthConnectClient.getOrCreate(ctx) }

    /** Total steps în intervalul [start, end) */
    suspend fun readStepsInputs(start: Instant, end: Instant): Int {
        val resp = client.readRecords(
            ReadRecordsRequest(
                StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        return resp.records.sumOf { it.count.toInt() }
    }

    /**
     * Minute de somn din zi, taiate la [start, end).
     */
    suspend fun readSleepMinutesClipped(start: Instant, end: Instant): Int {
        val queryStart = start.minus(Duration.ofHours(20)) // prinde sesiuni începute aseară
        val resp = client.readRecords(
            ReadRecordsRequest(
                SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(queryStart, end)
            )
        )
        val totalSecs = resp.records.sumOf { session ->
            val overlapStart = maxOf(session.startTime, start)
            val overlapEnd   = minOf(session.endTime, end)
            val secs = Duration.between(overlapStart, overlapEnd).seconds
            if (secs > 0) secs else 0
        }
        return (totalSecs / 60).toInt()
    }

    /**
     * Minute de somn care includ și porțiunea dinainte de miezul nopții
     * pentru sesiunile care trec peste 00:00.
     * Exemplu: 23:30→07:00 => se contorizează 23:30→07:00 (nu doar 00:00→07:00).
     */
    suspend fun readSleepMinutesIncludingPrev(start: Instant, end: Instant, backtrackHours: Long = 20): Int {
        val queryStart = start.minus(Duration.ofHours(backtrackHours))
        val resp = client.readRecords(
            ReadRecordsRequest(
                SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(queryStart, end)
            )
        )
        val totalSecs = resp.records.sumOf { s ->
            if (s.endTime <= start) 0L
            else {
                val effectiveEnd = if (s.endTime > end) end else s.endTime
                val secs = Duration.between(s.startTime, effectiveEnd).seconds
                if (secs > 0) secs else 0
            }
        }
        return (totalSecs / 60).toInt()
    }
}
