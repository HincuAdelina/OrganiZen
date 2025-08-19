package com.organizen.app.home.models

data class Message(
    val type: MessageType,
    val message: String
) {

}

enum class MessageType {
    USER, TOOL,
}