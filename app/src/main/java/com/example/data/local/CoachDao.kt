package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CoachDao {
    // User Profile methods
    @Query("SELECT * FROM user_profiles ORDER BY createdAt DESC")
    fun getAllProfiles(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles ORDER BY createdAt DESC")
    suspend fun getAllProfilesList(): List<UserProfileEntity>

    @Query("SELECT * FROM user_profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE id = :id")
    fun getProfileFlow(id: Long): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: UserProfileEntity)

    @Query("DELETE FROM user_profiles WHERE id = :id")
    suspend fun deleteProfile(id: Long)

    // Chat Message methods (profile aware)
    @Query("SELECT * FROM chat_messages WHERE profileId = :profileId ORDER BY timestamp ASC")
    fun getMessagesForProfile(profileId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE profileId = :profileId")
    suspend fun clearChatHistoryForProfile(profileId: Long)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatHistory()

    // Grammar Tips methods
    @Query("SELECT * FROM grammar_tips WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun getGrammarTipsForProfile(profileId: Long): Flow<List<GrammarTipEntity>>

    @Query("SELECT * FROM grammar_tips ORDER BY timestamp DESC")
    fun getAllGrammarTips(): Flow<List<GrammarTipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrammarTip(tip: GrammarTipEntity): Long

    @Update
    suspend fun updateGrammarTip(tip: GrammarTipEntity)

    @Query("DELETE FROM grammar_tips WHERE id = :tipId")
    suspend fun deleteGrammarTip(tipId: Long)

    @Query("SELECT COUNT(*) FROM chat_messages WHERE profileId = :profileId AND sender = 'USER'")
    fun getUserMessageCountForProfile(profileId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM grammar_tips WHERE profileId = :profileId")
    fun getGrammarTipCountForProfile(profileId: Long): Flow<Int>

    // Saved Words methods
    @Query("SELECT * FROM saved_words WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun getSavedWordsForProfile(profileId: Long): Flow<List<SavedWordEntity>>

    @Query("SELECT * FROM saved_words ORDER BY timestamp DESC")
    fun getAllSavedWords(): Flow<List<SavedWordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedWord(word: SavedWordEntity): Long

    @Update
    suspend fun updateSavedWord(word: SavedWordEntity)

    @Query("DELETE FROM saved_words WHERE id = :id")
    suspend fun deleteSavedWord(id: Long)

    @Query("SELECT COUNT(*) FROM saved_words WHERE profileId = :profileId")
    fun getSavedWordCountForProfile(profileId: Long): Flow<Int>

    // User Memory methods
    @Query("SELECT * FROM user_memory WHERE profileId = :profileId")
    fun getUserMemoryForProfileFlow(profileId: Long): Flow<UserMemoryEntity?>

    @Query("SELECT * FROM user_memory WHERE profileId = :profileId")
    suspend fun getUserMemoryForProfile(profileId: Long): UserMemoryEntity?

    @Query("SELECT * FROM user_memory WHERE profileId = 1")
    fun getUserMemoryFlow(): Flow<UserMemoryEntity?>

    @Query("SELECT * FROM user_memory WHERE profileId = 1")
    suspend fun getUserMemory(): UserMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserMemory(memory: UserMemoryEntity)

    // Scenario Session methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenarioSession(session: ScenarioSessionEntity)

    @Update
    suspend fun updateScenarioSession(session: ScenarioSessionEntity)

    @Query("SELECT * FROM scenario_sessions WHERE sessionId = :sessionId")
    suspend fun getScenarioSession(sessionId: String): ScenarioSessionEntity?

    @Query("SELECT * FROM scenario_sessions WHERE profileId = :profileId AND status = 'IN_PROGRESS' ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSessionForProfile(profileId: Long): ScenarioSessionEntity?

    @Query("SELECT * FROM scenario_sessions WHERE profileId = :profileId ORDER BY startedAt DESC")
    fun getScenarioSessionsForProfile(profileId: Long): Flow<List<ScenarioSessionEntity>>

    // Scenario Turns & Goals
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenarioTurn(turn: ScenarioTurnEntity): Long

    @Query("SELECT * FROM scenario_turns WHERE sessionId = :sessionId ORDER BY turnIndex ASC")
    fun getTurnsForSession(sessionId: String): Flow<List<ScenarioTurnEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalProgress(progress: ScenarioGoalProgressEntity): Long

    @Query("SELECT * FROM scenario_goal_progress WHERE sessionId = :sessionId")
    fun getGoalProgressForSession(sessionId: String): Flow<List<ScenarioGoalProgressEntity>>

    // Corrections & Review Queue
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorrection(correction: CorrectionEntity): Long

    @Query("SELECT * FROM corrections WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun getCorrectionsForProfile(profileId: Long): Flow<List<CorrectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewItem(item: ReviewItemEntity): Long

    @Update
    suspend fun updateReviewItem(item: ReviewItemEntity)

    @Query("SELECT * FROM review_items WHERE profileId = :profileId AND isMastered = 0 AND nextReviewAt <= :now ORDER BY nextReviewAt ASC")
    fun getDueReviewItemsForProfile(profileId: Long, now: Long = System.currentTimeMillis()): Flow<List<ReviewItemEntity>>

    // Daily Activities & Skill Snapshots
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyActivity(activity: DailyActivityEntity): Long

    @Query("SELECT * FROM daily_activities WHERE profileId = :profileId AND dateKey = :dateKey")
    suspend fun getDailyActivity(profileId: Long, dateKey: String): DailyActivityEntity?

    @Query("SELECT * FROM daily_activities WHERE profileId = :profileId ORDER BY dateKey DESC LIMIT 30")
    fun getRecentActivitiesForProfile(profileId: Long): Flow<List<DailyActivityEntity>>
}

