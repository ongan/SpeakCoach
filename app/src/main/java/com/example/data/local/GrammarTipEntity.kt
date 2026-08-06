package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grammar_tips")
data class GrammarTipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalSentence: String,
    val correctedSentence: String,
    val explanation: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isMastered: Boolean = false
)
