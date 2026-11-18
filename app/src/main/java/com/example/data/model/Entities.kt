package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pets")
data class PetProfile(
    @PrimaryKey val id: String,
    val name: String,
    val breed: String,
    val ageYears: Int,
    val ageMonths: Int,
    val sex: String = "Female",
    val reproductiveStatus: String = "Spayed / Neutered",
    val weightKg: Double = 4.2,
    val coatTags: String = "Ginger / Orange",
    val calibratedPct: Int = 100,
    val samplesAnalyzed: Int = 34,
    val baselinePitchHz: Int = 420,
    val vocalCadence: String = "Melodic",
    val dominantMood: String = "Playful & Demanding",
    val birthday: String = "Oct 12, 2022",
    val microchipId: String = "985141002341882",
    val vetName: String = "Dr. Sarah Adams",
    val vetClinic: String = "Metropolitan Pet Clinic",
    val vetPhone: String = "5550192834",
    val avatarKey: String = "lucy",
    val customAvatarUri: String? = null,
    val isActive: Boolean = false
) {
    val ageDisplay: String
        get() = when {
            ageYears > 0 && ageMonths > 0 -> "$ageYears yr${if (ageYears > 1) "s" else ""} $ageMonths mo${if (ageMonths > 1) "s" else ""}"
            ageYears > 0 -> "$ageYears yr${if (ageYears > 1) "s" else ""}"
            else -> "$ageMonths mo${if (ageMonths > 1) "s" else ""}"
        }
}

@Entity(tableName = "translations")
data class TranslationRecord(
    @PrimaryKey val id: String,
    val createdAtMillis: Long,
    val timestampLabel: String,
    val catName: String,
    val location: String,
    val title: String,
    val titleFa: String,
    val intentCode: String,
    val intentTag: String,
    val confidence: Int,
    val pitchHz: String,
    val durationSec: String,
    val isBookmarked: Boolean = true,
    val audioPath: String? = null,
    val sampleAssetName: String? = null,
    val waveformCsv: String = "0.25,0.45,0.8,0.95,0.65,0.4,0.25,0.6,0.85,0.9,0.6,0.35",
    val feedbackStatus: String? = null // "Confirmed", "Corrected: <intent>", or null
)

@Entity(tableName = "feedback_logs")
data class FeedbackLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAtMillis: Long,
    val translationId: String?,
    val predictedIntent: String,
    val userVerdict: String, // "Correct" or "Wrong"
    val userCorrectedIntent: String
)
