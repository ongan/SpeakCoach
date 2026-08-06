package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String, // "USER" or "COACH"
    val text: String,
    val feedback: String? = null, // Grammar error correction if coach message
    val timestamp: Long = System.currentTimeMillis(),
    val isVoice: Boolean = false,
    val scenario: String? = null
)
