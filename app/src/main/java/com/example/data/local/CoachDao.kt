package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CoachDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatHistory()

    @Query("SELECT * FROM grammar_tips ORDER BY timestamp DESC")
    fun getAllGrammarTips(): Flow<List<GrammarTipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrammarTip(tip: GrammarTipEntity): Long

    @Update
    suspend fun updateGrammarTip(tip: GrammarTipEntity)

    @Query("DELETE FROM grammar_tips WHERE id = :tipId")
    suspend fun deleteGrammarTip(tipId: Long)

    @Query("SELECT COUNT(*) FROM chat_messages WHERE sender = 'USER'")
    fun getUserMessageCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM grammar_tips")
    fun getGrammarTipCount(): Flow<Int>

    @Query("SELECT * FROM saved_words ORDER BY timestamp DESC")
    fun getAllSavedWords(): Flow<List<SavedWordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedWord(word: SavedWordEntity): Long

    @Update
    suspend fun updateSavedWord(word: SavedWordEntity)

    @Query("DELETE FROM saved_words WHERE id = :id")
    suspend fun deleteSavedWord(id: Long)

    @Query("SELECT COUNT(*) FROM saved_words")
    fun getSavedWordCount(): Flow<Int>

    @Query("SELECT * FROM user_memory WHERE id = 1")
    fun getUserMemoryFlow(): Flow<UserMemoryEntity?>

    @Query("SELECT * FROM user_memory WHERE id = 1")
    suspend fun getUserMemory(): UserMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserMemory(memory: UserMemoryEntity)
}
