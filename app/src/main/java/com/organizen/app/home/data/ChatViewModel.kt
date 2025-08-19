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
}

data class ChatViewState(
    val messages: List<Message> = emptyList(),
    val agentIsTyping: Boolean = false
)
