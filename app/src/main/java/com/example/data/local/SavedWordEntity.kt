package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_words")
data class SavedWordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val meaning: String,
    val exampleSentence: String,
    val contextSentence: String? = null,
    val isMastered: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
