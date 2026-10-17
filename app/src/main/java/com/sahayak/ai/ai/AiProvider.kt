package com.sahayak.ai.ai

data class ChatTurn(val fromUser: Boolean, val text: String)

interface AiProvider {
    suspend fun chat(history: List<ChatTurn>, userMessage: String): String
    suspend fun testConnection(): String
}

class AiException(message: String) : Exception(message)
