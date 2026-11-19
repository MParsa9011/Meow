package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AppLanguage
import com.example.data.model.AppSettings
import com.example.data.model.ModelEngineType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("meow_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun load(): AppSettings {
        return AppSettings(
            language = AppLanguage.fromCode(prefs.getString("language", AppLanguage.FA.code)),
            modelEngine = ModelEngineType.fromId(prefs.getString("model", "hybrid") ?: "hybrid"),
            sensitivityLevel = prefs.getString("sensitivity", "Medium") ?: "Medium",
            noiseFilterEnabled = prefs.getBoolean("noise_filter", true),
            continuousDetection = prefs.getBoolean("continuous_detection", true),
            autoRecordMeows = prefs.getBoolean("auto_record", true),
            audioQuality = prefs.getString("quality", "Studio HD") ?: "Studio HD",
            instantMeowAlerts = prefs.getBoolean("instant_alerts", true),
            dailyBehaviorSummary = prefs.getBoolean("daily_summary", true),
            feedingReminderSync = prefs.getBoolean("feeding_sync", false),
            saveAudioLocally = prefs.getBoolean("save_locally", true),
            showPersianSubtitles = prefs.getBoolean("show_persian", true),
            isProMember = prefs.getBoolean("is_pro", false),
            onboardingCompleted = prefs.getBoolean("onboarding_completed", false)
        )
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        prefs.edit()
            .putString("language", next.language.code)
            .putString("model", next.modelEngine.id)
            .putString("sensitivity", next.sensitivityLevel)
            .putBoolean("noise_filter", next.noiseFilterEnabled)
            .putBoolean("continuous_detection", next.continuousDetection)
            .putBoolean("auto_record", next.autoRecordMeows)
            .putString("quality", next.audioQuality)
            .putBoolean("instant_alerts", next.instantMeowAlerts)
            .putBoolean("daily_summary", next.dailyBehaviorSummary)
            .putBoolean("feeding_sync", next.feedingReminderSync)
            .putBoolean("save_locally", next.saveAudioLocally)
            .putBoolean("show_persian", next.showPersianSubtitles)
            .putBoolean("is_pro", next.isProMember)
            .putBoolean("onboarding_completed", next.onboardingCompleted)
            .apply()
    }
}
