package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val nativeLanguage: String = "Türkçe",
    val cefrLevel: String = "CEFR B1",
    val interests: String = "",
    val autoPlayTts: Boolean = true,
    val avatarColorIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
