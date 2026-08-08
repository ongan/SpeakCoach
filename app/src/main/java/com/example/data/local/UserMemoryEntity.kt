package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_memory")
data class UserMemoryEntity(
    @PrimaryKey
    val profileId: Long = 1,
    val interests: String = "",
    val conversationSummary: String = "",
    val learnedFacts: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

