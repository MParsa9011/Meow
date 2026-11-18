package com.example.data.model

data class AppSettings(
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
    val isProMember: Boolean = false
)
