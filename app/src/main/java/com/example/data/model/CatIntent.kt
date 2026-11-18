package com.example.data.model

enum class IntentCode(val code: String) {
    BRUSHING("B"),
    FOOD("F"),
    ISOLATION("I");

    companion object {
        fun fromCode(code: String): IntentCode = when (code.uppercase()) {
            "F" -> FOOD
            "I" -> ISOLATION
            else -> BRUSHING
        }
    }
}

enum class ModelEngineType(
    val id: String,
    val displayName: String,
    val accuracyLabel: String,
    val subtitle: String
) {
    HYBRID(
        id = "hybrid",
        displayName = "Hybrid Late Fusion AI",
        accuracyLabel = "79.4% Acc",
        subtitle = "Blends 234 Bioacoustic features (45%) + Mel-CNN (55%)"
    ),
    ACOUSTIC(
        id = "acoustic",
        displayName = "Enhanced Bioacoustic Pipeline",
        accuracyLabel = "74.6% Acc",
        subtitle = "YIN pitch contour, MFCCs, spectral flux & logistic classifier"
    ),
    CNN(
        id = "cnn",
        displayName = "Spectrogram CNN",
        accuracyLabel = "76.2% Acc",
        subtitle = "128-bin Mel Spectrogram 2D convolutional neural network"
    );

    companion object {
        fun fromId(id: String): ModelEngineType =
            entries.find { it.id == id } ?: HYBRID
    }
}

data class IntentMatch(
    val rank: Int,
    val intentCode: IntentCode,
    val title: String,
    val titleFa: String,
    val description: String,
    val descriptionFa: String,
    val percentage: Int,
    val badge: String,
    val intentTag: String,
    val pitchHz: String,
    val durationSec: String,
    val careTipFa: String
)

data class VoiceAnalysisResult(
    val topMatch: IntentMatch,
    val matches: List<IntentMatch>,
    val rawDurationSec: Double,
    val cleanDurationSec: Double,
    val pitchHz: Double,
    val rmsEnergy: Double,
    val peakPos: Double,
    val modelUsed: String,
    val confidence: Int,
    val waveformBars: List<Float>,
    val audioFilePath: String? = null,
    val sampleAssetName: String? = null
)

data class BundledSample(
    val id: String,
    val title: String,
    val shortLabel: String,
    val assetFilename: String,
    val intentCode: IntentCode,
    val catNote: String
)
