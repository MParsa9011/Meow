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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

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
    version = 1,
    exportSchema = false
)
abstract class MeowDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao
    abstract fun translationDao(): TranslationDao
    abstract fun feedbackDao(): FeedbackDao

    companion object {
        @Volatile
        private var INSTANCE: MeowDatabase? = null

        fun getInstance(context: Context): MeowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MeowDatabase::class.java,
                    "meow_database.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getInstance(context)
                                seedInitialData(database)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedInitialData(db: MeowDatabase) {
            val now = System.currentTimeMillis()
            val defaultPets = listOf(
                PetProfile(
                    id = "lucy",
                    name = "Lucy",
                    breed = "Domestic Shorthair",
                    ageYears = 2,
                    ageMonths = 3,
                    sex = "Female",
                    reproductiveStatus = "Spayed / Neutered",
                    weightKg = 4.2,
                    coatTags = "Ginger / Orange, Classic Tabby",
                    calibratedPct = 100,
                    samplesAnalyzed = 34,
                    baselinePitchHz = 420,
                    vocalCadence = "Melodic",
                    dominantMood = "Playful & Demanding",
                    birthday = "Oct 12, 2022",
                    microchipId = "985141002341882",
                    vetName = "Dr. Sarah Adams",
                    vetClinic = "Metropolitan Pet Clinic",
                    vetPhone = "5550192834",
                    avatarKey = "lucy",
                    isActive = true
                ),
                PetProfile(
                    id = "milo",
                    name = "Milo",
                    breed = "Ginger Tabby",
                    ageYears = 1,
                    ageMonths = 8,
                    sex = "Male",
                    reproductiveStatus = "Spayed / Neutered",
                    weightKg = 4.8,
                    coatTags = "Ginger / Orange",
                    calibratedPct = 92,
                    samplesAnalyzed = 18,
                    baselinePitchHz = 510,
                    vocalCadence = "Chirpy Trill",
                    dominantMood = "Food Motivated",
                    birthday = "Mar 19, 2023",
                    microchipId = "985141008914221",
                    vetName = "Dr. Sarah Adams",
                    vetClinic = "Metropolitan Pet Clinic",
                    vetPhone = "5550192834",
                    avatarKey = "milo",
                    isActive = false
                ),
                PetProfile(
                    id = "oliver",
                    name = "Oliver",
                    breed = "British Shorthair",
                    ageYears = 3,
                    ageMonths = 0,
                    sex = "Male",
                    reproductiveStatus = "Spayed / Neutered",
                    weightKg = 5.6,
                    coatTags = "Solid Blue-Grey",
                    calibratedPct = 85,
                    samplesAnalyzed = 12,
                    baselinePitchHz = 365,
                    vocalCadence = "Deep & Calm",
                    dominantMood = "Affection & Purring",
                    birthday = "Nov 05, 2021",
                    microchipId = "985141006621094",
                    vetName = "Dr. Sarah Adams",
                    vetClinic = "Metropolitan Pet Clinic",
                    vetPhone = "5550192834",
                    avatarKey = "oliver",
                    isActive = false
                )
            )
            db.petDao().insertAll(defaultPets)

            val defaultHistory = listOf(
                TranslationRecord(
                    id = "hist-1",
                    createdAtMillis = now - 3_600_000L * 2,
                    timestampLabel = "Today, 8:30 AM",
                    catName = "Lucy",
                    location = "Kitchen",
                    title = "Hungry & Wants Food!",
                    titleFa = "درخواست غذا و خوراکی",
                    intentCode = "F",
                    intentTag = "Hunger",
                    confidence = 88,
                    pitchHz = "680 Hz",
                    durationSec = "1.8s",
                    isBookmarked = true,
                    sampleAssetName = "F_MIN01_EU_FN_BEN01_102.wav",
                    waveformCsv = "0.25,0.42,0.75,0.95,0.68,0.45,0.3,0.62,0.88,0.78,0.52,0.28",
                    feedbackStatus = "Confirmed"
                ),
                TranslationRecord(
                    id = "hist-2",
                    createdAtMillis = now - 86_400_000L,
                    timestampLabel = "Yesterday, 6:15 PM",
                    catName = "Lucy",
                    location = "Living Room",
                    title = "Affection & Brushing Care",
                    titleFa = "نوازش، آرامش و شانه زدن",
                    intentCode = "B",
                    intentTag = "Affection",
                    confidence = 76,
                    pitchHz = "540 Hz",
                    durationSec = "2.3s",
                    isBookmarked = true,
                    sampleAssetName = "B_BRA01_MC_MN_SIM01_301.wav",
                    waveformCsv = "0.2,0.35,0.52,0.65,0.72,0.6,0.48,0.38,0.55,0.62,0.42,0.22",
                    feedbackStatus = null
                ),
                TranslationRecord(
                    id = "hist-3",
                    createdAtMillis = now - 86_400_000L * 2,
                    timestampLabel = "2 days ago, 11:40 AM",
                    catName = "Lucy",
                    location = "Hallway",
                    title = "Isolation, Calling or Distress",
                    titleFa = "انزوا، احساس تنهایی یا اضطراب",
                    intentCode = "I",
                    intentTag = "Needs Care",
                    confidence = 71,
                    pitchHz = "410 Hz",
                    durationSec = "2.1s",
                    isBookmarked = false,
                    sampleAssetName = "I_MIN01_EU_FN_BEN01_106.wav",
                    waveformCsv = "0.18,0.32,0.68,0.82,0.58,0.35,0.28,0.42,0.65,0.4,0.25,0.15",
                    feedbackStatus = null
                )
            )
            db.translationDao().insertAll(defaultHistory)
        }
    }
}
