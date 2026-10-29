package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val sender: String, // "user" or "nova"
    val text: String,
    val emotion: String = "neutral", // "warm", "excited", "empathetic", "playful", "serious", "neutral"
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey
    val sessionId: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
