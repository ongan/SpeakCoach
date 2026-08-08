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
    val learningGoal: String = "Daily Life", // travel, career, daily life, exam, academic, relocation
    val dailyPracticeMinutes: Int = 10, // 5, 10, 15, 20
    val interests: String = "",
    val correctionIntensity: String = "BALANCED", // FLOW, BALANCED, COACH
    val selectedCoach: String = "MAYA", // MAYA, LEO
    val interfaceMode: String = "HYBRID", // IMMERSIVE_CALL, HYBRID, CHAT_ONLY
    val speechRate: Float = 1.0f,
    val autoPlayTts: Boolean = true,
    val avatarColorIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
