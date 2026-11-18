package com.example.audio

import android.content.Context
import com.example.data.model.BundledSample
import com.example.data.model.IntentCode
import com.example.data.model.IntentMatch
import com.example.data.model.ModelEngineType
import com.example.data.model.VoiceAnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

class CatAiInferenceEngine(private val context: Context) {

    private data class LoadedWeights(
        val wAcoustic: Double,
        val featureNames: List<String>,
        val scalerMean: DoubleArray,
        val scalerScale: DoubleArray,
        val selectedIndices: IntArray,
        val logregCoef: DoubleArray, // 3 * 200
        val logregIntercept: DoubleArray, // 3
        val conv1W: FloatArray, // 16 * 1 * 3 * 3
        val conv1B: FloatArray, // 16
        val bn1W: FloatArray,
        val bn1B: FloatArray,
        val bn1Mean: FloatArray,
        val bn1Var: FloatArray,
        val conv2W: FloatArray, // 32 * 16 * 3 * 3
        val conv2B: FloatArray, // 32
        val bn2W: FloatArray,
        val bn2B: FloatArray,
        val bn2Mean: FloatArray,
        val bn2Var: FloatArray,
        val fcW: FloatArray, // 3 * 8192
        val fcB: FloatArray // 3
    )

    @Volatile
    private var weights: LoadedWeights? = null

    val bundledSamples: List<BundledSample> = listOf(
        BundledSample(
            id = "food_sample",
            title = "Hungry Meow (Food)",
            shortLabel = "Hungry Meow",
            assetFilename = "F_MIN01_EU_FN_BEN01_102.wav",
            intentCode = IntentCode.FOOD,
            catNote = "MIN01 • Test Cat"
        ),
        BundledSample(
            id = "brushing_sample",
            title = "Happy Trill (Brushing)",
            shortLabel = "Happy Trill",
            assetFilename = "B_BRA01_MC_MN_SIM01_301.wav",
            intentCode = IntentCode.BRUSHING,
            catNote = "BRA01 • Test Cat"
        ),
        BundledSample(
            id = "isolation_sample",
            title = "Distress Call (Isolation)",
            shortLabel = "Distress Call",
            assetFilename = "I_MIN01_EU_FN_BEN01_106.wav",
            intentCode = IntentCode.ISOLATION,
            catNote = "MIN01 • Test Cat"
        )
    )

    private fun ensureLoaded(): LoadedWeights {
        weights?.let { return it }
        synchronized(this) {
            weights?.let { return it }
            val jsonText = context.assets.open("cat_ai_model.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonText)
            val cnn = root.getJSONObject("cnn")

            val loaded = LoadedWeights(
                wAcoustic = root.optDouble("w_acoustic", 0.45),
                featureNames = root.getJSONArray("feature_names").toStringList(),
                scalerMean = root.getJSONArray("scaler_mean").toDoubleArray(),
                scalerScale = root.getJSONArray("scaler_scale").toDoubleArray(),
                selectedIndices = root.getJSONArray("selected_indices").toIntArray(),
                logregCoef = root.getJSONArray("logreg_coef").toDoubleArray(),
                logregIntercept = root.getJSONArray("logreg_intercept").toDoubleArray(),
                conv1W = cnn.getJSONArray("conv1.weight").toFloatArray(),
                conv1B = cnn.getJSONArray("conv1.bias").toFloatArray(),
                bn1W = cnn.getJSONArray("bn1.weight").toFloatArray(),
                bn1B = cnn.getJSONArray("bn1.bias").toFloatArray(),
                bn1Mean = cnn.getJSONArray("bn1.running_mean").toFloatArray(),
                bn1Var = cnn.getJSONArray("bn1.running_var").toFloatArray(),
                conv2W = cnn.getJSONArray("conv2.weight").toFloatArray(),
                conv2B = cnn.getJSONArray("conv2.bias").toFloatArray(),
                bn2W = cnn.getJSONArray("bn2.weight").toFloatArray(),
                bn2B = cnn.getJSONArray("bn2.bias").toFloatArray(),
                bn2Mean = cnn.getJSONArray("bn2.running_mean").toFloatArray(),
                bn2Var = cnn.getJSONArray("bn2.running_var").toFloatArray(),
                fcW = cnn.getJSONArray("fc.weight").toFloatArray(),
                fcB = cnn.getJSONArray("fc.bias").toFloatArray()
            )
            weights = loaded
            return loaded
        }
    }

    suspend fun analyzeAudio(
        rawSamples8k: FloatArray,
        engineType: ModelEngineType,
        noiseFilterEnabled: Boolean = true,
        audioFilePath: String? = null,
        sampleAssetName: String? = null
    ): VoiceAnalysisResult = withContext(Dispatchers.Default) {
        val w = ensureLoaded()
        val rawDuration = rawSamples8k.size.toDouble() / AudioDsp.TARGET_SR
        val yClean = if (noiseFilterEnabled) {
            AudioDsp.denoiseAudio(rawSamples8k, AudioDsp.TARGET_SR)
        } else {
            AudioDsp.trimSilence(rawSamples8k)
        }
        val cleanDuration = yClean.size.toDouble() / AudioDsp.TARGET_SR

        // Real YIN pitch estimation
        val f0 = AudioDsp.estimateYinPitch(yClean, AudioDsp.TARGET_SR)
        val validF0 = f0.filter { it in 160.0..1950.0 }.sorted()
        val pitchHz = if (validF0.size > 2) validF0[validF0.size / 2] else 450.0

        val rmsFrames = AudioDsp.computeRmsFrames(yClean, frameLength = 512, hopLength = 128)
        val rmsEnergy = if (rmsFrames.isNotEmpty()) rmsFrames.average() else 0.05
        val peakIdx = rmsFrames.indices.maxByOrNull { rmsFrames[it] } ?: 0
        val peakPos = peakIdx.toDouble() / max(1, rmsFrames.size)

        // 1. Acoustic Branch (234 features -> StandardScaler -> SelectKBest -> LogisticRegression)
        val pAcoustic = runAcousticPipeline(rawSamples8k, w)

        // 2. CNN Branch (128x32 Mel Spectrogram -> 2x Conv/BN/ReLU/MaxPool -> Linear)
        val pCnn = runSpectrogramCnn(yClean, w)

        // 3. Model Selection / Hybrid Late Fusion
        val blended = when (engineType) {
            ModelEngineType.CNN -> pCnn
            ModelEngineType.ACOUSTIC -> pAcoustic
            ModelEngineType.HYBRID -> DoubleArray(3) { i ->
                w.wAcoustic * pAcoustic[i] + (1.0 - w.wAcoustic) * pCnn[i]
            }
        }

        // Order: 0 = B, 1 = F, 2 = I
        val classOrder = listOf(
            IntentCode.BRUSHING to blended[0],
            IntentCode.FOOD to blended[1],
            IntentCode.ISOLATION to blended[2]
        ).sortedByDescending { it.second }

        val matches = classOrder.mapIndexed { rank, (code, prob) ->
            buildIntentMatch(rank, code, prob, pitchHz, cleanDuration)
        }

        VoiceAnalysisResult(
            topMatch = matches.first(),
            matches = matches,
            rawDurationSec = (rawDuration * 100.0).roundToInt() / 100.0,
            cleanDurationSec = (cleanDuration * 100.0).roundToInt() / 100.0,
            pitchHz = (pitchHz * 10.0).roundToInt() / 10.0,
            rmsEnergy = (rmsEnergy * 10000.0).roundToInt() / 10000.0,
            peakPos = (peakPos * 100.0).roundToInt() / 100.0,
            modelUsed = "${engineType.displayName} (${engineType.accuracyLabel})",
            confidence = matches.first().percentage,
            waveformBars = AudioDsp.computeWaveformBars(yClean, barCount = 24),
            audioFilePath = audioFilePath,
            sampleAssetName = sampleAssetName
        )
    }

    private fun runAcousticPipeline(audio: FloatArray, w: LoadedWeights): DoubleArray {
        val featMap = AudioDsp.extract234Features(audio, AudioDsp.TARGET_SR)
        val nFeatures = w.featureNames.size
        val scaled = DoubleArray(nFeatures)
        for (i in 0 until nFeatures) {
            val rawVal = featMap[w.featureNames[i]] ?: 0.0
            val safeVal = if (rawVal.isFinite()) rawVal else 0.0
            val scale = if (w.scalerScale[i] > 1e-12) w.scalerScale[i] else 1.0
            scaled[i] = (safeVal - w.scalerMean[i]) / scale
        }

        val k = w.selectedIndices.size
        val logits = DoubleArray(3)
        for (c in 0 until 3) {
            var sum = w.logregIntercept[c]
            val rowOffset = c * k
            for (j in 0 until k) {
                val featIdx = w.selectedIndices[j]
                sum += w.logregCoef[rowOffset + j] * scaled[featIdx]
            }
            logits[c] = sum
        }
        return softmax(logits)
    }

    private fun runSpectrogramCnn(yClean: FloatArray, w: LoadedWeights): DoubleArray {
        val input = AudioDsp.computeMelSpectrogram128x32(yClean, AudioDsp.TARGET_SR) // 1 x 128 x 32

        // Layer 1: Conv2d(1 -> 16, k=3, pad=1) + BN + ReLU -> (16, 128, 32)
        val c1 = conv2d3x3Pad1(input, 1, 128, 32, w.conv1W, w.conv1B, 16)
        batchNormReluInPlace(c1, 16, 128 * 32, w.bn1W, w.bn1B, w.bn1Mean, w.bn1Var)
        // Pool 1: MaxPool2d(2,2) -> (16, 64, 16)
        val p1 = maxPool2x2(c1, 16, 128, 32)

        // Layer 2: Conv2d(16 -> 32, k=3, pad=1) + BN + ReLU -> (32, 64, 16)
        val c2 = conv2d3x3Pad1(p1, 16, 64, 16, w.conv2W, w.conv2B, 32)
        batchNormReluInPlace(c2, 32, 64 * 16, w.bn2W, w.bn2B, w.bn2Mean, w.bn2Var)
        // Pool 2: MaxPool2d(2,2) -> (32, 32, 8) = 8192
        val p2 = maxPool2x2(c2, 32, 64, 16)

        // FC: Linear(8192 -> 3)
        val logits = DoubleArray(3)
        val featLen = p2.size
        for (c in 0 until 3) {
            var sum = w.fcB[c].toDouble()
            val offset = c * featLen
            for (i in 0 until featLen) {
                sum += w.fcW[offset + i] * p2[i]
            }
            logits[c] = sum
        }
        return softmax(logits)
    }

    private fun conv2d3x3Pad1(
        input: FloatArray,
        inC: Int,
        h: Int,
        w: Int,
        weight: FloatArray,
        bias: FloatArray,
        outC: Int
    ): FloatArray {
        val hw = h * w
        val out = FloatArray(outC * hw)
        for (oc in 0 until outC) {
            val bVal = bias[oc]
            val outOffset = oc * hw
            val wOcOffset = oc * inC * 9
            for (y in 0 until h) {
                for (x in 0 until w) {
                    var sum = bVal
                    for (ic in 0 until inC) {
                        val inOffset = ic * hw
                        val kOffset = wOcOffset + ic * 9
                        for (ky in -1..1) {
                            val iy = y + ky
                            if (iy in 0 until h) {
                                val rowBase = inOffset + iy * w
                                val kRow = kOffset + (ky + 1) * 3
                                for (kx in -1..1) {
                                    val ix = x + kx
                                    if (ix in 0 until w) {
                                        sum += input[rowBase + ix] * weight[kRow + (kx + 1)]
                                    }
                                }
                            }
                        }
                    }
                    out[outOffset + y * w + x] = sum
                }
            }
        }
        return out
    }

    private fun batchNormReluInPlace(
        data: FloatArray,
        channels: Int,
        spatialSize: Int,
        gamma: FloatArray,
        beta: FloatArray,
        mean: FloatArray,
        varArr: FloatArray
    ) {
        val eps = 1e-5f
        for (c in 0 until channels) {
            val invStd = 1f / sqrt(varArr[c] + eps)
            val scale = gamma[c] * invStd
            val shift = beta[c] - mean[c] * scale
            val offset = c * spatialSize
            for (i in 0 until spatialSize) {
                val v = data[offset + i] * scale + shift
                data[offset + i] = if (v > 0f) v else 0f
            }
        }
    }

    private fun maxPool2x2(input: FloatArray, channels: Int, h: Int, w: Int): FloatArray {
        val outH = h / 2
        val outW = w / 2
        val out = FloatArray(channels * outH * outW)
        for (c in 0 until channels) {
            val inBase = c * h * w
            val outBase = c * outH * outW
            for (y in 0 until outH) {
                val iy = y * 2
                for (x in 0 until outW) {
                    val ix = x * 2
                    val v00 = input[inBase + iy * w + ix]
                    val v01 = input[inBase + iy * w + ix + 1]
                    val v10 = input[inBase + (iy + 1) * w + ix]
                    val v11 = input[inBase + (iy + 1) * w + ix + 1]
                    out[outBase + y * outW + x] = max(max(v00, v01), max(v10, v11))
                }
            }
        }
        return out
    }

    private fun softmax(logits: DoubleArray): DoubleArray {
        val maxVal = logits.maxOrNull() ?: 0.0
        val exps = DoubleArray(logits.size) { exp(logits[it] - maxVal) }
        val sum = exps.sum().coerceAtLeast(1e-12)
        return DoubleArray(logits.size) { exps[it] / sum }
    }

    private fun buildIntentMatch(
        rank: Int,
        code: IntentCode,
        prob: Double,
        pitchHz: Double,
        durationSec: Double
    ): IntentMatch {
        val pct = (prob * 100.0).roundToInt().coerceIn(0, 100)
        val badge = when (rank) {
            0 -> "Highest match"
            1 -> "Second match"
            else -> "Low match"
        }
        val roundedDuration = max(1.0, (durationSec * 10.0).roundToInt() / 10.0)
        return when (code) {
            IntentCode.FOOD -> IntentMatch(
                rank = rank + 1,
                intentCode = code,
                title = "Hungry & Wants Food!",
                titleFa = "درخواست غذا و خوراکی",
                description = "Direct request for feeding, wet treat bowl or fresh snack",
                descriptionFa = "درخواست صریح غذا، کنسرو یا خوراکی خوشمزه",
                percentage = pct,
                badge = badge,
                intentTag = "High Intent",
                pitchHz = "${if (pitchHz > 0) max(550, (pitchHz * 1.1).toInt()) else 680} Hz",
                durationSec = "${roundedDuration}s",
                careTipFa = "گربه شما در این حالت اغلب در نزدیکی ظرف غذا یا آشپزخانه میومیو می‌کند و صدای او دارای شیب فرکانسی صعودی است."
            )
            IntentCode.BRUSHING -> IntentMatch(
                rank = rank + 1,
                intentCode = code,
                title = "Affection & Brushing Care",
                titleFa = "نوازش، آرامش و شانه زدن",
                description = "Relaxed state seeking touch, sweet affection or grooming",
                descriptionFa = "حالت آرامش، لذت از شانه زدن موها و تمایل به نوازش",
                percentage = pct,
                badge = badge,
                intentTag = "Gentle Affection",
                pitchHz = "${if (pitchHz > 0) max(380, (pitchHz * 0.9).toInt()) else 520} Hz",
                durationSec = "${roundedDuration}s",
                careTipFa = "صدای این حالت معمولاً با ریتم یکنواخت، بدون لرزش‌های استرسی و همراه با خرخر ملایم همراه است."
            )
            IntentCode.ISOLATION -> IntentMatch(
                rank = rank + 1,
                intentCode = code,
                title = "Isolation, Calling or Distress",
                titleFa = "انزوا، احساس تنهایی یا اضطراب",
                description = "Seeking reassurance, lonely call or litter box check needed",
                descriptionFa = "احساس تنهایی، فراخوانی همراه با اضطراب یا بررسی جعبه خاک",
                percentage = pct,
                badge = badge,
                intentTag = "Needs Care",
                pitchHz = "${if (pitchHz > 0) pitchHz.toInt() else 420} Hz",
                durationSec = "${roundedDuration}s",
                careTipFa = "میومیوهای بلند با انحنای فرکانسی کشیده در این حالت رخ می‌دهند. لطفاً در و پنجره‌ها، آب تمیز و جعبه خاک را بررسی کنید."
            )
        }
    }

    private fun JSONArray.toStringList(): List<String> = List(length()) { getString(it) }
    private fun JSONArray.toDoubleArray(): DoubleArray = DoubleArray(length()) { getDouble(it) }
    private fun JSONArray.toIntArray(): IntArray = IntArray(length()) { getInt(it) }
    private fun JSONArray.toFloatArray(): FloatArray = FloatArray(length()) { getDouble(it).toFloat() }
}
