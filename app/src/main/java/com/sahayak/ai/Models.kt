package com.sahayak.ai

data class ChatMessage(val role: String, val text: String)

sealed class CommandResult {
    object NotLocal : CommandResult()
    object StopSpeaking : CommandResult()
    data class Local(val reply: String, val action: (() -> Unit)? = null) : CommandResult()
}
