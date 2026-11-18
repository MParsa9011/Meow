package com.example

import android.app.Application
import com.example.audio.AudioRecorderManager
import com.example.audio.CatAiInferenceEngine
import com.example.data.local.MeowDatabase
import com.example.data.local.MeowRepository
import com.example.data.local.SettingsStore

class MeowApplication : Application() {
    val database: MeowDatabase by lazy { MeowDatabase.getInstance(this) }
    val settingsStore: SettingsStore by lazy { SettingsStore(this) }
    val repository: MeowRepository by lazy { MeowRepository(this, database, settingsStore) }
    val aiEngine: CatAiInferenceEngine by lazy { CatAiInferenceEngine(this) }
    val audioManager: AudioRecorderManager by lazy { AudioRecorderManager(this) }
}
