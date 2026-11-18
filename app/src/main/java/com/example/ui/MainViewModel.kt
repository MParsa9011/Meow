package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioRecorderManager
import com.example.audio.CatAiInferenceEngine
import com.example.data.local.MeowRepository
import com.example.data.model.AppSettings
import com.example.data.model.BundledSample
import com.example.data.model.IntentCode
import com.example.data.model.ModelEngineType
import com.example.data.model.PetProfile
import com.example.data.model.TranslationRecord
import com.example.data.model.VoiceAnalysisResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class RecordingPhase {
    IDLE,
    RECORDING,
    ANALYZING,
    COMPLETE
}

data class TranslatorUiState(
    val phase: RecordingPhase = RecordingPhase.IDLE,
    val statusText: String = "Ready to listen for meows",
    val activeSampleId: String? = null,
    val analysisResult: VoiceAnalysisResult? = null,
    val currentTranslationId: String? = null,
    val isCurrentBookmarked: Boolean = false,
    val expandedMatchRank: Int = 1,
    val feedbackSubmitted: Boolean = false,
    val errorMessage: String? = null
)

class MainViewModel(
    private val repository: MeowRepository,
    private val aiEngine: CatAiInferenceEngine,
    private val audioManager: AudioRecorderManager
) : ViewModel() {

    val pets: StateFlow<List<PetProfile>> = repository.pets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePet: StateFlow<PetProfile?> = pets
        .combine(MutableStateFlow(Unit)) { list, _ ->
            list.firstOrNull { it.isActive } ?: list.firstOrNull()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val translations: StateFlow<List<TranslationRecord>> = repository.translations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val feedbackLogs = repository.feedbackLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = repository.settings

    val liveAmplitudes: StateFlow<List<Float>> = audioManager.liveAmplitudes
    val playingAudioId: StateFlow<String?> = audioManager.playingId
    val bundledSamples: List<BundledSample> = aiEngine.bundledSamples

    private val _translatorState = MutableStateFlow(TranslatorUiState())
    val translatorState: StateFlow<TranslatorUiState> = _translatorState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        // Pre-run analysis on the first bundled sample so the user sees live AI results immediately
        viewModelScope.launch {
            runCatching {
                val defaultSample = bundledSamples.first()
                val samples8k = audioManager.loadBundledSample(defaultSample.assetFilename)
                val result = aiEngine.analyzeAudio(
                    rawSamples8k = samples8k,
                    engineType = settings.value.modelEngine,
                    noiseFilterEnabled = settings.value.noiseFilterEnabled,
                    sampleAssetName = defaultSample.assetFilename
                )
                _translatorState.value = TranslatorUiState(
                    phase = RecordingPhase.COMPLETE,
                    statusText = "Sample ready • ${defaultSample.shortLabel}",
                    activeSampleId = defaultSample.id,
                    analysisResult = result,
                    currentTranslationId = "hist-1",
                    isCurrentBookmarked = true,
                    expandedMatchRank = 1
                )
            }
        }
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
        viewModelScope.launch {
            delay(3200)
            if (_toastMessage.value == msg) {
                _toastMessage.value = null
            }
        }
    }

    fun dismissToast() {
        _toastMessage.value = null
    }

    fun startRecording() {
        viewModelScope.launch {
            try {
                val sr = if (settings.value.audioQuality == "Studio HD") 16000 else 8000
                audioManager.startRecording(sr)
                _translatorState.value = _translatorState.value.copy(
                    phase = RecordingPhase.RECORDING,
                    statusText = "Listening for meow...",
                    activeSampleId = null,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _translatorState.value = _translatorState.value.copy(
                    phase = RecordingPhase.IDLE,
                    errorMessage = e.message ?: "Unable to start microphone"
                )
                showToast("Microphone unavailable: ${e.localizedMessage ?: "check permissions"}")
            }
        }
    }

    fun stopRecordingAndAnalyze() {
        viewModelScope.launch {
            _translatorState.value = _translatorState.value.copy(
                phase = RecordingPhase.ANALYZING,
                statusText = "Analyzing meow frequency..."
            )
            try {
                val (samples8k, savedPath) = audioManager.stopAndGetAudio(settings.value.saveAudioLocally)
                if (samples8k.size < 800) {
                    _translatorState.value = _translatorState.value.copy(
                        phase = RecordingPhase.IDLE,
                        statusText = "Recording too short — tap and hold closer",
                        errorMessage = "Audio clip was shorter than 0.1 seconds"
                    )
                    showToast("Audio too short — try recording for 1–3 seconds")
                    return@launch
                }
                analyzeAndRecord(
                    samples8k = samples8k,
                    audioPath = savedPath,
                    sampleAsset = null,
                    sampleId = null,
                    location = "Live Mic"
                )
            } catch (e: Exception) {
                _translatorState.value = _translatorState.value.copy(
                    phase = RecordingPhase.IDLE,
                    statusText = "Ready to listen for meows",
                    errorMessage = e.localizedMessage
                )
                showToast("Analysis error: ${e.localizedMessage ?: "unknown"}")
            }
        }
    }

    fun analyzeBundledSample(sample: BundledSample) {
        viewModelScope.launch {
            _translatorState.value = _translatorState.value.copy(
                phase = RecordingPhase.ANALYZING,
                statusText = "Analyzing ${sample.shortLabel}...",
                activeSampleId = sample.id,
                errorMessage = null
            )
            try {
                val samples8k = audioManager.loadBundledSample(sample.assetFilename)
                audioManager.playAudioSource("sample_${sample.id}", null, sample.assetFilename)
                analyzeAndRecord(
                    samples8k = samples8k,
                    audioPath = null,
                    sampleAsset = sample.assetFilename,
                    sampleId = sample.id,
                    location = sample.catNote
                )
            } catch (e: Exception) {
                _translatorState.value = _translatorState.value.copy(
                    phase = RecordingPhase.IDLE,
                    errorMessage = e.localizedMessage
                )
                showToast("Could not analyze sample")
            }
        }
    }

    fun analyzeUploadedUri(uri: Uri) {
        viewModelScope.launch {
            _translatorState.value = _translatorState.value.copy(
                phase = RecordingPhase.ANALYZING,
                statusText = "Decoding & analyzing audio...",
                activeSampleId = null,
                errorMessage = null
            )
            try {
                val (samples8k, savedPath) = audioManager.decodeAudioUri(uri, settings.value.saveAudioLocally)
                if (samples8k.size < 400) {
                    _translatorState.value = _translatorState.value.copy(
                        phase = RecordingPhase.IDLE,
                        statusText = "Ready to listen for meows"
                    )
                    showToast("Selected audio file appears empty")
                    return@launch
                }
                analyzeAndRecord(
                    samples8k = samples8k,
                    audioPath = savedPath,
                    sampleAsset = null,
                    sampleId = null,
                    location = "Uploaded Audio"
                )
            } catch (e: Exception) {
                _translatorState.value = _translatorState.value.copy(
                    phase = RecordingPhase.IDLE,
                    statusText = "Ready to listen for meows",
                    errorMessage = e.localizedMessage
                )
                showToast("Could not decode audio: ${e.localizedMessage ?: "unsupported format"}")
            }
        }
    }

    private suspend fun analyzeAndRecord(
        samples8k: FloatArray,
        audioPath: String?,
        sampleAsset: String?,
        sampleId: String?,
        location: String
    ) {
        val currentSettings = settings.value
        val result = aiEngine.analyzeAudio(
            rawSamples8k = samples8k,
            engineType = currentSettings.modelEngine,
            noiseFilterEnabled = currentSettings.noiseFilterEnabled,
            audioFilePath = audioPath,
            sampleAssetName = sampleAsset
        )

        val petName = activePet.value?.name ?: "Lucy"
        val top = result.topMatch
        val autoSave = currentSettings.autoRecordMeows && result.confidence >= 45
        val recordId = "hist-${UUID.randomUUID().toString().take(8)}"

        if (autoSave) {
            val now = System.currentTimeMillis()
            val timeLabel = "Today, ${SimpleDateFormat("h:mm a", Locale.US).format(Date(now))}"
            val record = TranslationRecord(
                id = recordId,
                createdAtMillis = now,
                timestampLabel = timeLabel,
                catName = petName,
                location = location,
                title = top.title,
                titleFa = top.titleFa,
                intentCode = top.intentCode.code,
                intentTag = MeowRepository.intentTagForCode(top.intentCode),
                confidence = result.confidence,
                pitchHz = top.pitchHz,
                durationSec = top.durationSec,
                isBookmarked = true,
                audioPath = audioPath,
                sampleAssetName = sampleAsset,
                waveformCsv = result.waveformBars.take(12).joinToString(",") { "%.2f".format(Locale.US, it) }
            )
            repository.saveTranslation(record)
            repository.incrementActivePetSampleCount()
        }

        _translatorState.value = TranslatorUiState(
            phase = RecordingPhase.COMPLETE,
            statusText = "Analysis complete",
            activeSampleId = sampleId,
            analysisResult = result,
            currentTranslationId = if (autoSave) recordId else null,
            isCurrentBookmarked = autoSave,
            expandedMatchRank = 1,
            feedbackSubmitted = false
        )
    }

    fun selectExpandedMatch(rank: Int) {
        _translatorState.value = _translatorState.value.copy(expandedMatchRank = rank)
    }

    fun toggleCurrentBookmark() {
        val state = _translatorState.value
        val result = state.analysisResult ?: return
        val petName = activePet.value?.name ?: "Lucy"
        viewModelScope.launch {
            val existingId = state.currentTranslationId
            if (existingId != null) {
                val next = !state.isCurrentBookmarked
                repository.toggleBookmark(existingId, next)
                _translatorState.value = state.copy(isCurrentBookmarked = next)
                showToast(
                    if (next) "Saved \"${result.topMatch.title}\" to $petName's history"
                    else "Removed bookmark from history"
                )
            } else {
                val newId = "hist-${UUID.randomUUID().toString().take(8)}"
                val now = System.currentTimeMillis()
                val timeLabel = "Today, ${SimpleDateFormat("h:mm a", Locale.US).format(Date(now))}"
                val top = result.topMatch
                repository.saveTranslation(
                    TranslationRecord(
                        id = newId,
                        createdAtMillis = now,
                        timestampLabel = timeLabel,
                        catName = petName,
                        location = "Translator",
                        title = top.title,
                        titleFa = top.titleFa,
                        intentCode = top.intentCode.code,
                        intentTag = MeowRepository.intentTagForCode(top.intentCode),
                        confidence = result.confidence,
                        pitchHz = top.pitchHz,
                        durationSec = top.durationSec,
                        isBookmarked = true,
                        audioPath = result.audioFilePath,
                        sampleAssetName = result.sampleAssetName,
                        waveformCsv = result.waveformBars.take(12).joinToString(",") { "%.2f".format(Locale.US, it) }
                    )
                )
                _translatorState.value = state.copy(currentTranslationId = newId, isCurrentBookmarked = true)
                showToast("Saved \"${top.title}\" to $petName's history")
            }
        }
    }

    fun submitFeedback(verdict: String, correctedCode: IntentCode?) {
        val state = _translatorState.value
        val top = state.analysisResult?.topMatch ?: return
        viewModelScope.launch {
            repository.submitFeedback(
                translationId = state.currentTranslationId,
                predictedIntent = top.title,
                userVerdict = verdict,
                correctedIntentCode = correctedCode
            )
            _translatorState.value = state.copy(feedbackSubmitted = true)
            showToast("Feedback logged ($verdict) for Active Learning")
        }
    }

    fun submitHistoryFeedback(record: TranslationRecord, verdict: String, correctedCode: IntentCode?) {
        viewModelScope.launch {
            repository.submitFeedback(
                translationId = record.id,
                predictedIntent = record.title,
                userVerdict = verdict,
                correctedIntentCode = correctedCode
            )
            showToast("Updated translation feedback ($verdict)")
        }
    }

    fun toggleHistoryBookmark(record: TranslationRecord) {
        viewModelScope.launch {
            repository.toggleBookmark(record.id, !record.isBookmarked)
        }
    }

    fun deleteHistoryRecord(id: String) {
        viewModelScope.launch {
            repository.deleteTranslation(id)
            showToast("Deleted translation entry")
        }
    }

    fun playCatResponse(playId: String, intentCode: IntentCode) {
        audioManager.playCatResponse(playId, intentCode)
    }

    fun playRecordedOrSample(playId: String, audioPath: String?, sampleAssetName: String?) {
        if (audioPath != null || sampleAssetName != null) {
            audioManager.playAudioSource(playId, audioPath, sampleAssetName)
        } else {
            showToast("No saved audio clip for this entry")
        }
    }

    fun switchActivePet(petId: String) {
        viewModelScope.launch {
            repository.setActivePet(petId)
            val chosen = pets.value.find { it.id == petId }
            if (chosen != null) showToast("Switched active cat to ${chosen.name}")
        }
    }

    fun savePet(pet: PetProfile, isNew: Boolean) {
        viewModelScope.launch {
            if (isNew) {
                repository.addPet(pet, setAsActive = true)
                showToast("${pet.name}'s voice profile calibrated & activated")
            } else {
                repository.updatePet(pet)
                showToast("Updated ${pet.name}'s profile")
            }
        }
    }

    fun updatePetCustomAvatar(uri: Uri) {
        val current = activePet.value ?: return
        viewModelScope.launch {
            repository.updatePet(current.copy(customAvatarUri = uri.toString()))
            showToast("Updated ${current.name}'s portrait")
        }
    }

    fun setModelEngine(engine: ModelEngineType) {
        repository.updateSettings { it.copy(modelEngine = engine) }
        showToast("AI Engine: ${engine.displayName}")
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        repository.updateSettings(transform)
    }

    fun exportHistory(context: Context) {
        viewModelScope.launch {
            try {
                val file = repository.exportHistoryCsv(translations.value)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Meow Translation History CSV")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Export Translation History"))
            } catch (e: Exception) {
                showToast("Export failed: ${e.localizedMessage ?: "error"}")
            }
        }
    }

    fun clearHistoryAndCache() {
        viewModelScope.launch {
            val bytes = repository.clearHistoryAndCache()
            val kb = bytes / 1024
            showToast("Cleared history & ${kb} KB of cached audio")
        }
    }

    class Factory(
        private val repository: MeowRepository,
        private val aiEngine: CatAiInferenceEngine,
        private val audioManager: AudioRecorderManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository, aiEngine, audioManager) as T
        }
    }
}
