package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.FeedbackLog
import com.example.data.model.PetProfile
import com.example.data.model.TranslationRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Query("SELECT * FROM pets ORDER BY isActive DESC, name ASC")
    fun observeAllPets(): Flow<List<PetProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPet(pet: PetProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pets: List<PetProfile>)

    @Update
    suspend fun updatePet(pet: PetProfile)

    @Query("UPDATE pets SET isActive = (id = :petId)")
    suspend fun setActivePet(petId: String)

    @Query("UPDATE pets SET samplesAnalyzed = samplesAnalyzed + 1 WHERE isActive = 1")
    suspend fun incrementActivePetSampleCount()
}

@Dao
interface TranslationDao {
    @Query("SELECT * FROM translations ORDER BY createdAtMillis DESC")
    fun observeAllTranslations(): Flow<List<TranslationRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslation(record: TranslationRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<TranslationRecord>)

    @Query("UPDATE translations SET isBookmarked = :bookmarked WHERE id = :id")
    suspend fun updateBookmark(id: String, bookmarked: Boolean)

    @Query("UPDATE translations SET feedbackStatus = :status WHERE id = :id")
    suspend fun updateFeedbackStatus(id: String, status: String)

    @Query("UPDATE translations SET intentCode = :intentCode, intentTag = :intentTag, title = :title, titleFa = :titleFa, feedbackStatus = :feedbackStatus WHERE id = :id")
    suspend fun updateCorrectedIntent(
        id: String,
        intentCode: String,
        intentTag: String,
        title: String,
        titleFa: String,
        feedbackStatus: String
    )

    @Query("DELETE FROM translations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM translations")
    suspend fun clearAll()
}

@Dao
interface FeedbackDao {
    @Query("SELECT * FROM feedback_logs ORDER BY createdAtMillis DESC")
    fun observeAllFeedback(): Flow<List<FeedbackLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(log: FeedbackLog)

    @Query("DELETE FROM feedback_logs")
    suspend fun clearAll()
}

@Database(
    entities = [PetProfile::class, TranslationRecord::class, FeedbackLog::class],
    version = 2,
    exportSchema = false
)
abstract class MeowDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao
    abstract fun translationDao(): TranslationDao
    abstract fun feedbackDao(): FeedbackDao

    companion object {
        @Volatile
        private var INSTANCE: MeowDatabase? = null

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Remove legacy demo history rows from v1 installs while preserving user recordings
                db.execSQL("DELETE FROM translations WHERE id IN ('hist-1', 'hist-2', 'hist-3')")
            }
        }

        fun getInstance(context: Context): MeowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MeowDatabase::class.java,
                    "meow_database.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
