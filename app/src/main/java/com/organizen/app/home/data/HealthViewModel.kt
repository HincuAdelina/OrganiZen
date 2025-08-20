package com.organizen.app.home.data

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.llm.OllamaModels
import android.app.Application
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.organizen.app.BuildConfig
import kotlinx.coroutines.Dispatchers
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
    var stepsGoal by mutableStateOf(5000f)
        private set
    var sleepGoal by mutableStateOf(7.0)
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

    fun updateStepsGoal(value: Float) { stepsGoal = value }
    fun updateSleepGoal(value: Double) { sleepGoal = value }

    fun recommend() {
        val agent = AIAgent(
            executor = simpleOllamaAIExecutor(BuildConfig.default_account_iccid),
            systemPrompt = "You are a helpful assistant. Answer user questions concisely.",
            llmModel = OllamaModels.Meta.LLAMA_3_2_3B,
            toolRegistry = ToolRegistry {
                tools(CalculatorTools())
            },
        )

        viewModelScope.launch(Dispatchers.IO) {
            println(agent.run("What's 1 + 2"))
        }
    }
}


// Implement a simple calculator tool that can add two numbers
@LLMDescription("Tools for performing basic arithmetic operations")
class CalculatorTools : ToolSet {
    @Tool
    @LLMDescription("Add two numbers together and return their sum")
    fun add(
        @LLMDescription("First number to add (integer value)")
        num1: Int,

        @LLMDescription("Second number to add (integer value)")
        num2: Int
    ): String {
        val sum = num1 + num2
        return "The sum of $num1 and $num2 is: $sum"
    }
}