package com.example.data.local

import android.content.Context
import com.example.data.model.AppSettings
import com.example.data.model.FeedbackLog
import com.example.data.model.IntentCode
import com.example.data.model.PetProfile
import com.example.data.model.TranslationRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

class MeowRepository(
    private val context: Context,
    private val database: MeowDatabase,
    private val settingsStore: SettingsStore
) {
    val pets: Flow<List<PetProfile>> = database.petDao().observeAllPets()
    val translations: Flow<List<TranslationRecord>> = database.translationDao().observeAllTranslations()
    val feedbackLogs: Flow<List<FeedbackLog>> = database.feedbackDao().observeAllFeedback()
    val settings: StateFlow<AppSettings> = settingsStore.settings

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settingsStore.update(transform)
    }

    suspend fun setActivePet(petId: String) {
        database.petDao().setActivePet(petId)
    }

    suspend fun addPet(pet: PetProfile, setAsActive: Boolean = true) {
        database.petDao().insertPet(pet.copy(isActive = setAsActive))
        if (setAsActive) {
            database.petDao().setActivePet(pet.id)
        }
    }

    suspend fun updatePet(pet: PetProfile) {
        database.petDao().updatePet(pet)
    }

    suspend fun incrementActivePetSampleCount() {
        database.petDao().incrementActivePetSampleCount()
    }

    suspend fun saveTranslation(record: TranslationRecord) {
        database.translationDao().insertTranslation(record)
    }

    suspend fun toggleBookmark(id: String, bookmarked: Boolean) {
        database.translationDao().updateBookmark(id, bookmarked)
    }

    suspend fun deleteTranslation(id: String) {
        database.translationDao().deleteById(id)
    }

    suspend fun clearHistoryAndCache(): Long = withContext(Dispatchers.IO) {
        database.translationDao().clearAll()
        database.feedbackDao().clearAll()
        var freedBytes = 0L
        val recDir = File(context.filesDir, "recordings")
        if (recDir.exists()) {
            recDir.listFiles()?.forEach { f ->
                freedBytes += f.length()
                f.delete()
            }
        }
        val exportDir = File(context.cacheDir, "exports")
        if (exportDir.exists()) {
            exportDir.listFiles()?.forEach { f ->
                freedBytes += f.length()
                f.delete()
            }
        }
        freedBytes
    }

    suspend fun submitFeedback(
        translationId: String?,
        predictedIntent: String,
        userVerdict: String,
        correctedIntentCode: IntentCode?
    ) {
        val correctedLabel = if (userVerdict == "Wrong" && correctedIntentCode != null) {
            intentTitleForCode(correctedIntentCode)
        } else {
            "N/A"
        }

        database.feedbackDao().insertFeedback(
            FeedbackLog(
                createdAtMillis = System.currentTimeMillis(),
                translationId = translationId,
                predictedIntent = predictedIntent,
                userVerdict = userVerdict,
                userCorrectedIntent = correctedLabel
            )
        )

        if (translationId != null) {
            if (userVerdict == "Correct") {
                database.translationDao().updateFeedbackStatus(translationId, "Confirmed")
            } else if (correctedIntentCode != null) {
                database.translationDao().updateCorrectedIntent(
                    id = translationId,
                    intentCode = correctedIntentCode.code,
                    intentTag = intentTagForCode(correctedIntentCode),
                    title = intentTitleForCode(correctedIntentCode),
                    titleFa = intentTitleFaForCode(correctedIntentCode),
                    feedbackStatus = "Corrected: ${intentTagForCode(correctedIntentCode)}"
                )
            }
        }
    }

    suspend fun exportHistoryCsv(records: List<TranslationRecord>): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val csvFile = File(exportDir, "meow_translation_history.csv")
        val sb = StringBuilder()
        sb.appendLine("id,timestamp,cat_name,location,intent_code,intent_tag,title,title_fa,confidence_pct,pitch_hz,duration_sec,bookmarked,feedback_status")
        records.forEach { r ->
            val safeTitle = r.title.replace("\"", "\"\"")
            val safeTitleFa = r.titleFa.replace("\"", "\"\"")
            val safeFb = (r.feedbackStatus ?: "").replace("\"", "\"\"")
            sb.appendLine(
                "${r.id},\"${r.timestampLabel}\",\"${r.catName}\",\"${r.location}\",${r.intentCode},\"${r.intentTag}\",\"$safeTitle\",\"$safeTitleFa\",${r.confidence},\"${r.pitchHz}\",\"${r.durationSec}\",${r.isBookmarked},\"$safeFb\""
            )
        }
        csvFile.writeText(sb.toString(), Charsets.UTF_8)
        csvFile
    }

    companion object {
        fun intentTitleForCode(code: IntentCode): String = when (code) {
            IntentCode.FOOD -> "Hungry & Wants Food!"
            IntentCode.BRUSHING -> "Affection & Brushing Care"
            IntentCode.ISOLATION -> "Isolation, Calling or Distress"
        }

        fun intentTitleFaForCode(code: IntentCode): String = when (code) {
            IntentCode.FOOD -> "درخواست غذا و خوراکی"
            IntentCode.BRUSHING -> "نوازش، آرامش و شانه زدن"
            IntentCode.ISOLATION -> "انزوا، احساس تنهایی یا اضطراب"
        }

        fun intentTagForCode(code: IntentCode): String = when (code) {
            IntentCode.FOOD -> "Hunger"
            IntentCode.BRUSHING -> "Affection"
            IntentCode.ISOLATION -> "Needs Care"
        }
    }
}
