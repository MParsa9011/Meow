package com.example.data.model

enum class AppLanguage(val code: String, val displayName: String) {
    FA("fa", "فارسی"),
    EN("en", "English");

    companion object {
        fun fromCode(code: String?): AppLanguage = when (code?.lowercase()) {
            "en" -> EN
            else -> FA
        }
    }
}

data class AppSettings(
    val language: AppLanguage = AppLanguage.FA,
    val modelEngine: ModelEngineType = ModelEngineType.HYBRID,
    val sensitivityLevel: String = "Medium", // Low, Medium, High
    val noiseFilterEnabled: Boolean = true,
    val continuousDetection: Boolean = true,
    val autoRecordMeows: Boolean = true,
    val audioQuality: String = "Studio HD", // Studio HD, Balanced 16kHz
    val instantMeowAlerts: Boolean = true,
    val dailyBehaviorSummary: Boolean = true,
    val feedingReminderSync: Boolean = false,
    val saveAudioLocally: Boolean = true,
    val showPersianSubtitles: Boolean = true,
    val isProMember: Boolean = false,
    val onboardingCompleted: Boolean = false
)
