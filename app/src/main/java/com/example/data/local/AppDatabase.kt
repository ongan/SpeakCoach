package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChatMessageEntity::class,
        GrammarTipEntity::class,
        SavedWordEntity::class,
        UserMemoryEntity::class,
        UserProfileEntity::class,
        ScenarioSessionEntity::class,
        ScenarioTurnEntity::class,
        ScenarioGoalProgressEntity::class,
        CorrectionEntity::class,
        VocabularyEncounterEntity::class,
        ReviewItemEntity::class,
        DailyActivityEntity::class,
        SkillSnapshotEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coachDao(): CoachDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "speakcoach_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
