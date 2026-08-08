package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scenario_sessions",
    indices = [
        Index("profileId"),
        Index("scenarioId"),
        Index("status")
    ]
)
data class ScenarioSessionEntity(
    @PrimaryKey val sessionId: String,
    val profileId: Long,
    val scenarioId: String,
    val stage: String, // OPENING, DISCOVERY, COMPLICATION, RESOLUTION, REVIEW
    val selectedVariablesJson: String = "{}", // JSON map
    val turnCount: Int = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
    val status: String = "IN_PROGRESS", // IN_PROGRESS, COMPLETED, ABANDONED
    val finalScore: Int = 100,
    val summaryText: String? = null
)

@Entity(
    tableName = "scenario_turns",
    indices = [
        Index("sessionId"),
        Index("profileId"),
        Index("timestamp")
    ]
)
data class ScenarioTurnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val profileId: Long,
    val turnIndex: Int,
    val userText: String,
    val coachReply: String,
    val coachIntent: String, // REACT, ASK, INFORM, COMPLICATE, HELP, CLOSE
    val stage: String,
    val rawJsonResponse: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "scenario_goal_progress",
    indices = [
        Index("sessionId"),
        Index("goalId")
    ]
)
data class ScenarioGoalProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val goalId: String,
    val status: String, // NOT_STARTED, IN_PROGRESS, COMPLETED
    val evidenceText: String? = null,
    val completedAt: Long? = null
)

@Entity(
    tableName = "corrections",
    indices = [
        Index("profileId"),
        Index("sessionId"),
        Index("timestamp")
    ]
)
data class CorrectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val sessionId: String?,
    val originalSentence: String,
    val correctedSentence: String,
    val explanationNative: String,
    val category: String = "GRAMMAR", // GRAMMAR, VOCABULARY, NATURALNESS
    val severity: String = "IMPORTANT", // MINOR, IMPORTANT
    val isReviewed: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "vocabulary_encounters",
    indices = [
        Index("profileId"),
        Index("normalizedWord")
    ]
)
data class VocabularyEncounterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val normalizedWord: String,
    val originalWord: String,
    val contextSentence: String,
    val encounterCount: Int = 1,
    val lastEncounterAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "review_items",
    indices = [
        Index("profileId"),
        Index("nextReviewAt"),
        Index("itemType")
    ]
)
data class ReviewItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val itemType: String, // WORD, CORRECTION, PHRASE
    val prompt: String,
    val expectedAnswer: String,
    val explanation: String,
    val repetitions: Int = 0,
    val intervalDays: Int = 1,
    val easeFactor: Float = 2.5f,
    val nextReviewAt: Long = System.currentTimeMillis(),
    val lastReviewedAt: Long? = null,
    val isMastered: Boolean = false
)

@Entity(
    tableName = "daily_activities",
    indices = [
        Index("profileId"),
        Index("dateKey")
    ]
)
data class DailyActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val dateKey: String, // YYYY-MM-DD
    val speakingSeconds: Int = 0,
    val turnsCount: Int = 0,
    val sessionsCompleted: Int = 0,
    val goalsCompleted: Int = 0,
    val reviewItemsDone: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "skill_snapshots",
    indices = [
        Index("profileId"),
        Index("timestamp")
    ]
)
data class SkillSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val estimatedCefr: String,
    val fluencyProxyScore: Int,
    val grammarAccuracyScore: Int,
    val vocabBreadthScore: Int,
    val totalSpeakingMinutes: Int,
    val timestamp: Long = System.currentTimeMillis()
)
