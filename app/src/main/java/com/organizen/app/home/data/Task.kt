package com.organizen.app.home.data

import java.time.LocalDate

enum class Difficulty { EASY, MEDIUM, HARD }

enum class Tag { RELAXARE, INVATARE, SPORT }

data class Task(
    val id: Long = System.currentTimeMillis(),
    val description: String,
    val difficulty: Difficulty,
    val estimatedMinutes: Int,
    val tags: List<Tag>,
    val deadline: LocalDate,
    val completed: Boolean = false
)
