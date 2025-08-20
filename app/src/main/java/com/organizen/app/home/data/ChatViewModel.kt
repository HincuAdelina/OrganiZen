package com.organizen.app.home.data

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.llm.OllamaModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.BuildConfig
import com.organizen.app.home.models.Message
import com.organizen.app.home.models.MessageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class ChatViewModel: ViewModel() {
    var uiState: MutableStateFlow<ChatViewState> = MutableStateFlow(ChatViewState())

    private fun addMessage(string: String, type: MessageType, loading: Boolean? = null) {
        uiState.update {
            val a = it.messages.toMutableList()
            a.add(
                Message(type, string)
            )
            if (loading != null) {
                it.copy(messages = a, agentIsTyping = loading)
            } else {
                it.copy(messages = a)
            }
        }
    }

    fun sendMessage(string: String) {
        if (uiState.value.agentIsTyping) {
            return
        }
        addMessage(string, MessageType.USER)
        uiState.update {
            it.copy(agentIsTyping = true)
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = simpleOllamaAIExecutor(com.organizen.app.BuildConfig.default_account_iccid)
                val response = client.execute(
                    prompt = prompt("prompt") {
                        system("You are a helpful assistant.")
                        uiState.value.messages.forEach {
                            if (it.type == MessageType.USER) {
                                user(it.message)
                            } else {
                                assistant(it.message)
                            }
                        }
                    },
                    model = OllamaModels.Meta.LLAMA_3_2_3B,
                    tools = emptyList()
                ).firstOrNull()
                addMessage(response?.content ?: "Could not reply", MessageType.TOOL, false)
            } catch (exception: Exception) {
                exception.printStackTrace()
                uiState.update {
                    it.copy(agentIsTyping = false)
                }
            }
        }
    }

    fun sendProductivityReport(tasks: List<Task>, steps: Long?, sleepHours: Double?) {
        if (uiState.value.agentIsTyping) {
            return
        }
        addMessage("How productive I was today?", MessageType.USER)
        uiState.update { it.copy(agentIsTyping = true) }
        viewModelScope.launch(Dispatchers.Default) {
            val today = LocalDate.now()
            val completedToday = tasks.filter { it.completed && it.deadline == today }
            val taskPoints = completedToday.sumOf {
                when (it.difficulty) {
                    Difficulty.EASY -> 1
                    Difficulty.MEDIUM -> 2
                    Difficulty.HARD -> 3
                }
            }
            val stepsGoal = 5000.0
            val sleepGoal = 7.0
            val stepsScore = (steps ?: 0L) / stepsGoal
            val sleepScore = (sleepHours ?: 0.0) / sleepGoal
            val productivityScore = taskPoints + stepsScore + sleepScore
            val breakdown = completedToday.groupingBy { it.difficulty }
                .eachCount()
                .entries.joinToString(", ") {
                    "${it.value} ${it.key.name.lowercase()}"
                }
            val reply = buildString {
                append("You've completed ${completedToday.size} tasks today")
                if (breakdown.isNotBlank()) append(" (" + breakdown + ")")
                append(". Steps: ${(steps ?: 0L)}/5000, Sleep: ${String.format("%.1f", sleepHours ?: 0.0)}h of 7h.")
                append(" Productivity score: ${String.format("%.2f", productivityScore)}")
            }
            addMessage(reply, MessageType.TOOL, false)
        }
    }
}

data class ChatViewState(
    val messages: List<Message> = emptyList(),
    val agentIsTyping: Boolean = false
)
