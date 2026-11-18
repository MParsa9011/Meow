package com.example.audio

import kotlin.math.*

object AudioDsp {
    const val TARGET_SR = 8000
    const val TARGET_SAMPLES = 16000 // 2.0 seconds at 8kHz

    fun resampleLinear(input: FloatArray, srcRate: Int, dstRate: Int): FloatArray {
        if (srcRate == dstRate || input.isEmpty()) return input
        val ratio = srcRate.toDouble() / dstRate.toDouble()
        val outLen = max(1, (input.size / ratio).toInt())
        val out = FloatArray(outLen)
        for (i in 0 until outLen) {
            val pos = i * ratio
            val idx = pos.toInt().coerceIn(0, input.size - 1)
            val frac = (pos - idx).toFloat()
            val next = if (idx + 1 < input.size) input[idx + 1] else input[idx]
            out[i] = input[idx] * (1f - frac) + next * frac
        }
        return out
    }

    /**
     * Matches features.py denoise_audio:
     * 1. DC offset removal
     * 2. 2nd-order Butterworth high-pass filter at 70 Hz (forward-backward)
     * 3. Energy-based silence trimming (top_db = 25)
     */
    fun denoiseAudio(y: FloatArray, sr: Int = TARGET_SR): FloatArray {
        if (y.isEmpty()) return y
        var mean = 0.0
        for (v in y) mean += v
        mean /= y.size
        val dcRemoved = DoubleArray(y.size) { y[it] - mean }

        val filtered = butterworthHighPass70Hz(dcRemoved, sr)
        val asFloat = FloatArray(filtered.size) { filtered[it].toFloat() }
        val trimmed = trimSilence(asFloat, topDb = 25.0)
        return if (trimmed.size >= (sr * 0.25).toInt()) trimmed else asFloat
    }

    private fun butterworthHighPass70Hz(x: DoubleArray, sr: Int): DoubleArray {
        if (x.size < 12) return x
        val fc = 70.0
        val k = tan(PI * fc / sr)
        val sqrt2 = sqrt(2.0)
        val norm = 1.0 / (1.0 + sqrt2 * k + k * k)
        val b0 = 1.0 * norm
        val b1 = -2.0 * norm
        val b2 = 1.0 * norm
        val a1 = 2.0 * (k * k - 1.0) * norm
        val a2 = (1.0 - sqrt2 * k + k * k) * norm

        val forward = biquadFilter(x, b0, b1, b2, a1, a2)
        val reversed = forward.reversedArray()
        val backward = biquadFilter(reversed, b0, b1, b2, a1, a2)
        return backward.reversedArray()
    }

    private fun biquadFilter(
        x: DoubleArray,
        b0: Double,
        b1: Double,
        b2: Double,
        a1: Double,
        a2: Double
    ): DoubleArray {
        val y = DoubleArray(x.size)
        var x1 = 0.0
        var x2 = 0.0
        var y1 = 0.0
        var y2 = 0.0
        for (i in x.indices) {
            val xi = x[i]
            val yi = b0 * xi + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            y[i] = yi
            x2 = x1
            x1 = xi
            y2 = y1
            y1 = yi
        }
        return y
    }

    fun trimSilence(
        y: FloatArray,
        topDb: Double = 25.0,
        frameLength: Int = 512,
        hopLength: Int = 128
    ): FloatArray {
        if (y.size <= frameLength) return y
        val rmsFrames = computeRmsFrames(y, frameLength, hopLength)
        if (rmsFrames.isEmpty()) return y
        val maxRms = rmsFrames.maxOrNull() ?: 0.0
        if (maxRms <= 1e-9) return y
        val threshold = maxRms * 10.0.pow(-topDb / 20.0)

        var startFrame = -1
        var endFrame = -1
        for (i in rmsFrames.indices) {
            if (rmsFrames[i] >= threshold) {
                if (startFrame == -1) startFrame = i
                endFrame = i
            }
        }
        if (startFrame == -1) return y
        val startSample = (startFrame * hopLength).coerceIn(0, y.size - 1)
        val endSample = ((endFrame * hopLength) + frameLength).coerceIn(startSample + 1, y.size)
        return y.copyOfRange(startSample, endSample)
    }

    fun computeWaveformBars(y: FloatArray, barCount: Int = 24): List<Float> {
        if (y.isEmpty()) return List(barCount) { 0.15f }
        val step = max(1, y.size / barCount)
        val bars = ArrayList<Float>(barCount)
        var maxPeak = 1e-6f
        for (i in 0 until barCount) {
            val start = (i * step).coerceAtMost(y.size - 1)
            val end = ((i + 1) * step).coerceAtMost(y.size)
            var sumSq = 0f
            for (j in start until end) {
                sumSq += y[j] * y[j]
            }
            val rms = sqrt(sumSq / max(1, end - start))
            if (rms > maxPeak) maxPeak = rms
            bars.add(rms)
        }
        return bars.map { (it / maxPeak).coerceIn(0.12f, 1.0f) }
    }

    fun computeRmsFrames(
        y: FloatArray,
        frameLength: Int = 512,
        hopLength: Int = 256
    ): DoubleArray {
        if (y.isEmpty()) return doubleArrayOf(0.0)
        val nFrames = max(1, 1 + (y.size - frameLength) / hopLength)
        val out = DoubleArray(nFrames)
        for (f in 0 until nFrames) {
            val start = f * hopLength
            val end = min(y.size, start + frameLength)
            var sumSq = 0.0
            for (i in start until end) {
                val v = y[i].toDouble()
                sumSq += v * v
            }
            out[f] = sqrt(sumSq / max(1, end - start))
        }
        return out
    }

    /**
     * YIN fundamental frequency estimator (150Hz - 2000Hz)
     */
    fun estimateYinPitch(
        y: FloatArray,
        sr: Int = TARGET_SR,
        fMin: Double = 150.0,
        fMax: Double = 2000.0,
        frameLength: Int = 1024,
        hopLength: Int = 256
    ): DoubleArray {
        if (y.size < frameLength) return doubleArrayOf()
        val tauMin = max(2, (sr / fMax).toInt())
        val tauMax = min(frameLength / 2 - 1, (sr / fMin).toInt())
        if (tauMax <= tauMin) return doubleArrayOf()

        val nFrames = max(1, 1 + (y.size - frameLength) / hopLength)
        val pitches = DoubleArray(nFrames)
        val diff = DoubleArray(tauMax + 1)
        val cmnd = DoubleArray(tauMax + 1)

        for (f in 0 until nFrames) {
            val offset = f * hopLength
            if (offset + frameLength > y.size) break
            diff.fill(0.0)
            for (tau in 1..tauMax) {
                var sum = 0.0
                for (j in 0 until frameLength - tauMax) {
                    val d = y[offset + j].toDouble() - y[offset + j + tau].toDouble()
                    sum += d * d
                }
                diff[tau] = sum
            }
            cmnd[0] = 1.0
            var runningSum = 0.0
            for (tau in 1..tauMax) {
                runningSum += diff[tau]
                cmnd[tau] = if (runningSum > 0.0) diff[tau] * tau / runningSum else 1.0
            }

            var bestTau = -1
            for (tau in tauMin..tauMax) {
                if (cmnd[tau] < 0.15) {
                    var t = tau
                    while (t + 1 <= tauMax && cmnd[t + 1] < cmnd[t]) {
                        t++
                    }
                    bestTau = t
                    break
                }
            }
            if (bestTau == -1) {
                var minVal = Double.MAX_VALUE
                for (tau in tauMin..tauMax) {
                    if (cmnd[tau] < minVal) {
                        minVal = cmnd[tau]
                        bestTau = tau
                    }
                }
            }
            pitches[f] = if (bestTau > 0) sr.toDouble() / bestTau else 0.0
        }
        return pitches
    }

    /**
     * Radix-2 Cooley-Tukey FFT (in-place). n must be power of 2.
     */
    private fun fftRadix2(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = real[i]; real[i] = real[j]; real[j] = tr
                val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti
            }
        }
        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wLenR = cos(ang)
            val wLenI = sin(ang)
            var i = 0
            while (i < n) {
                var wR = 1.0
                var wI = 0.0
                for (k in 0 until len / 2) {
                    val uR = real[i + k]
                    val uI = imag[i + k]
                    val vR = real[i + k + len / 2] * wR - imag[i + k + len / 2] * wI
                    val vI = real[i + k + len / 2] * wI + imag[i + k + len / 2] * wR
                    real[i + k] = uR + vR
                    imag[i + k] = uI + vI
                    real[i + k + len / 2] = uR - vR
                    imag[i + k + len / 2] = uI - vI
                    val nextWR = wR * wLenR - wI * wLenI
                    wI = wR * wLenI + wI * wLenR
                    wR = nextWR
                }
                i += len
            }
            len = len shl 1
        }
    }

    private fun hannWindow(n: Int): DoubleArray =
        DoubleArray(n) { 0.5 * (1.0 - cos(2.0 * PI * it / n)) }

    private fun stftPowerSpectrogram(
        y: FloatArray,
        nFft: Int,
        hopLength: Int,
        center: Boolean = true
    ): Array<DoubleArray> {
        val padded = if (center) {
            val pad = nFft / 2
            FloatArray(y.size + 2 * pad).also { arr ->
                for (i in y.indices) arr[pad + i] = y[i]
            }
        } else {
            if (y.size >= nFft) y else y.copyOf(nFft)
        }
        val nFrames = max(1, 1 + (padded.size - nFft) / hopLength)
        val nBins = nFft / 2 + 1
        val window = hannWindow(nFft)
        val real = DoubleArray(nFft)
        val imag = DoubleArray(nFft)
        val spec = Array(nBins) { DoubleArray(nFrames) }

        for (f in 0 until nFrames) {
            val start = f * hopLength
            real.fill(0.0)
            imag.fill(0.0)
            for (i in 0 until nFft) {
                val sample = if (start + i < padded.size) padded[start + i].toDouble() else 0.0
                real[i] = sample * window[i]
            }
            fftRadix2(real, imag)
            for (b in 0 until nBins) {
                spec[b][f] = real[b] * real[b] + imag[b] * imag[b]
            }
        }
        return spec
    }

    private fun hzToMelSlaney(hz: Double): Double {
        val fMin = 0.0
        val fSp = 200.0 / 3.0
        val minLogHz = 1000.0
        val minLogMel = (minLogHz - fMin) / fSp
        val logStep = ln(6.4) / 27.0
        return if (hz >= minLogHz) {
            minLogMel + ln(hz / minLogHz) / logStep
        } else {
            (hz - fMin) / fSp
        }
    }

    private fun melToHzSlaney(mel: Double): Double {
        val fMin = 0.0
        val fSp = 200.0 / 3.0
        val minLogHz = 1000.0
        val minLogMel = (minLogHz - fMin) / fSp
        val logStep = ln(6.4) / 27.0
        return if (mel >= minLogMel) {
            minLogHz * exp(logStep * (mel - minLogMel))
        } else {
            fMin + fSp * mel
        }
    }

    private fun buildMelFilterbank(sr: Int, nFft: Int, nMels: Int): Array<DoubleArray> {
        val nBins = nFft / 2 + 1
        val fMax = sr / 2.0
        val melMin = hzToMelSlaney(0.0)
        val melMax = hzToMelSlaney(fMax)
        val melPoints = DoubleArray(nMels + 2) { i ->
            melMin + i * (melMax - melMin) / (nMels + 1)
        }
        val hzPoints = DoubleArray(nMels + 2) { melToHzSlaney(melPoints[it]) }
        val fftFreqs = DoubleArray(nBins) { it * (sr / 2.0) / (nBins - 1) }

        val weights = Array(nMels) { DoubleArray(nBins) }
        for (m in 0 until nMels) {
            val fLeft = hzPoints[m]
            val fCenter = hzPoints[m + 1]
            val fRight = hzPoints[m + 2]
            val enorm = 2.0 / max(1e-9, fRight - fLeft)
            for (b in 0 until nBins) {
                val freq = fftFreqs[b]
                val lower = (freq - fLeft) / max(1e-9, fCenter - fLeft)
                val upper = (fRight - freq) / max(1e-9, fRight - fCenter)
                weights[m][b] = max(0.0, min(lower, upper)) * enorm
            }
        }
        return weights
    }

    /**
     * Computes 128 x 32 normalized Mel Spectrogram matching CatMeowCNN training:
     * target_samples = 16000 (2.0s at 8kHz), n_mels = 128, hop_length = 512, n_fft = 2048
     */
    fun computeMelSpectrogram128x32(yClean: FloatArray, sr: Int = TARGET_SR): FloatArray {
        val padded = FloatArray(TARGET_SAMPLES)
        val copyLen = min(yClean.size, TARGET_SAMPLES)
        System.arraycopy(yClean, 0, padded, 0, copyLen)

        val nFft = 2048
        val hopLength = 512
        val nMels = 128
        val powerSpec = stftPowerSpectrogram(padded, nFft = nFft, hopLength = hopLength, center = true)
        val melBank = buildMelFilterbank(sr, nFft, nMels)
        val nFrames = min(32, powerSpec[0].size)
        val nBins = powerSpec.size

        val melDb = DoubleArray(nMels * 32)
        var maxPower = 1e-10
        val melRaw = DoubleArray(nMels * 32)
        for (m in 0 until nMels) {
            val bank = melBank[m]
            for (t in 0 until nFrames) {
                var sum = 0.0
                for (b in 0 until nBins) {
                    sum += bank[b] * powerSpec[b][t]
                }
                melRaw[m * 32 + t] = sum
                if (sum > maxPower) maxPower = sum
            }
        }

        var minDb = Double.MAX_VALUE
        var maxDb = -Double.MAX_VALUE
        for (i in melRaw.indices) {
            val ratio = max(1e-10, melRaw[i]) / maxPower
            val db = max(-80.0, 10.0 * log10(ratio))
            melDb[i] = db
            if (db < minDb) minDb = db
            if (db > maxDb) maxDb = db
        }

        val denom = (maxDb - minDb) + 1e-6
        return FloatArray(melDb.size) { ((melDb[it] - minDb) / denom).toFloat() }
    }

    /**
     * Extracts the 234 bioacoustic & spectral features matching features.py
     */
    fun extract234Features(y: FloatArray, sr: Int = TARGET_SR): Map<String, Double> {
        val feats = LinkedHashMap<String, Double>(250)
        if (y.isEmpty()) return feats

        val rawDuration = y.size.toDouble() / sr
        val yTrimmed = trimSilence(y, topDb = 25.0).let {
            if (it.size < (sr * 0.2).toInt()) y else it
        }
        val yClean = denoiseAudio(y, sr)

        // 1. Temporal & Energy
        val duration = yTrimmed.size.toDouble() / sr
        feats["duration"] = duration
        feats["duration_ratio"] = duration / (rawDuration + 1e-6)

        val rms = computeRmsFrames(yTrimmed, frameLength = 512, hopLength = 256)
        feats["rms_mean"] = rms.average()
        feats["rms_std"] = std(rms)
        feats["rms_max"] = rms.maxOrNull() ?: 0.0
        feats["rms_skew"] = skewness(rms)

        val zcr = computeZcrFrames(yTrimmed, frameLength = 512, hopLength = 256)
        feats["zcr_mean"] = zcr.average()
        feats["zcr_std"] = std(zcr)

        // 2. Spectral Features (n_fft=512, hop=256)
        val nFft = 512
        val hop = 256
        val powerSpec = stftPowerSpectrogram(yTrimmed, nFft = nFft, hopLength = hop, center = true)
        val nBins = powerSpec.size
        val nFrames = powerSpec[0].size
        val freqs = DoubleArray(nBins) { it * (sr / 2.0) / (nBins - 1) }

        val magSpec = Array(nBins) { b ->
            DoubleArray(nFrames) { t -> sqrt(max(0.0, powerSpec[b][t])) }
        }

        val cent = DoubleArray(nFrames)
        val bw = DoubleArray(nFrames)
        val ro85 = DoubleArray(nFrames)
        val ro95 = DoubleArray(nFrames)
        val flatness = DoubleArray(nFrames)

        for (t in 0 until nFrames) {
            var magSum = 0.0
            var weightedFreq = 0.0
            var logSum = 0.0
            var arithSum = 0.0
            var totalEnergy = 0.0
            for (b in 0 until nBins) {
                val m = magSpec[b][t]
                val p = powerSpec[b][t] + 1e-12
                magSum += m
                weightedFreq += freqs[b] * m
                logSum += ln(p)
                arithSum += p
                totalEnergy += p
            }
            val c = if (magSum > 1e-12) weightedFreq / magSum else 0.0
            cent[t] = c

            var bwSum = 0.0
            var cum = 0.0
            var f85 = freqs.last()
            var f95 = freqs.last()
            var found85 = false
            var found95 = false
            for (b in 0 until nBins) {
                val m = magSpec[b][t]
                bwSum += m * (freqs[b] - c) * (freqs[b] - c)
                cum += powerSpec[b][t] + 1e-12
                if (!found85 && cum >= 0.85 * totalEnergy) {
                    f85 = freqs[b]; found85 = true
                }
                if (!found95 && cum >= 0.95 * totalEnergy) {
                    f95 = freqs[b]; found95 = true
                }
            }
            bw[t] = if (magSum > 1e-12) sqrt(bwSum / magSum) else 0.0
            ro85[t] = f85
            ro95[t] = f95
            val geoMean = exp(logSum / nBins)
            val arithMean = arithSum / nBins
            flatness[t] = if (arithMean > 1e-12) geoMean / arithMean else 0.0
        }

        feats["cent_mean"] = cent.average()
        feats["cent_std"] = std(cent)
        feats["cent_min"] = cent.minOrNull() ?: 0.0
        feats["cent_max"] = cent.maxOrNull() ?: 0.0

        feats["bw_mean"] = bw.average()
        feats["bw_std"] = std(bw)

        feats["rolloff85_mean"] = ro85.average()
        feats["rolloff85_std"] = std(ro85)

        feats["rolloff95_mean"] = ro95.average()
        feats["rolloff95_std"] = std(ro95)

        feats["flatness_mean"] = flatness.average()
        feats["flatness_std"] = std(flatness)

        // Spectral contrast (n_bands=4 => 5 sub-bands total)
        val bandEdges = doubleArrayOf(0.0, 100.0, 200.0, 400.0, 800.0, 4001.0)
        for (band in 0 until 5) {
            val lo = bandEdges[band]
            val hi = bandEdges[band + 1]
            val bandContrast = DoubleArray(nFrames)
            val binVals = ArrayList<Double>()
            for (t in 0 until nFrames) {
                binVals.clear()
                for (b in 0 until nBins) {
                    if (freqs[b] >= lo && freqs[b] < hi) {
                        binVals.add(magSpec[b][t])
                    }
                }
                if (binVals.size >= 2) {
                    binVals.sort()
                    val qCount = max(1, (binVals.size * 0.2).toInt())
                    val valley = binVals.take(qCount).average()
                    val peak = binVals.takeLast(qCount).average()
                    bandContrast[t] = 10.0 * log10((peak + 1e-9) / (valley + 1e-9))
                }
            }
            feats["contrast_mean_$band"] = bandContrast.average()
            feats["contrast_std_$band"] = std(bandContrast)
        }

        // 3. MFCCs 1..20 + Deltas + Delta2
        val nMelsMfcc = 40
        val melBankMfcc = buildMelFilterbank(sr, nFft, nMelsMfcc)
        val logMel = Array(nMelsMfcc) { DoubleArray(nFrames) }
        var maxM = 1e-10
        for (m in 0 until nMelsMfcc) {
            for (t in 0 until nFrames) {
                var s = 0.0
                for (b in 0 until nBins) s += melBankMfcc[m][b] * powerSpec[b][t]
                logMel[m][t] = s
                if (s > maxM) maxM = s
            }
        }
        for (m in 0 until nMelsMfcc) {
            for (t in 0 until nFrames) {
                logMel[m][t] = max(-80.0, 10.0 * log10(max(1e-10, logMel[m][t]) / maxM))
            }
        }
        val nMfcc = 20
        val mfcc = Array(nMfcc) { DoubleArray(nFrames) }
        val dctScale = sqrt(2.0 / nMelsMfcc)
        for (c in 0 until nMfcc) {
            val norm = if (c == 0) sqrt(1.0 / nMelsMfcc) else dctScale
            for (t in 0 until nFrames) {
                var sum = 0.0
                for (m in 0 until nMelsMfcc) {
                    sum += logMel[m][t] * cos(PI * c * (m + 0.5) / nMelsMfcc)
                }
                mfcc[c][t] = norm * sum
            }
        }
        val delta = computeDelta(mfcc)
        val delta2 = computeDelta(delta)
        for (i in 0 until nMfcc) {
            feats["mfcc_mean_$i"] = mfcc[i].average()
            feats["mfcc_std_$i"] = std(mfcc[i])
            feats["mfcc_max_$i"] = mfcc[i].maxOrNull() ?: 0.0
            feats["mfcc_min_$i"] = mfcc[i].minOrNull() ?: 0.0
            feats["mfcc_delta_mean_$i"] = delta[i].average()
            feats["mfcc_delta_std_$i"] = std(delta[i])
            feats["mfcc_delta2_mean_$i"] = delta2[i].average()
            feats["mfcc_delta2_std_$i"] = std(delta2[i])
        }

        // 4. F0 Pitch Contour on trimmed audio
        val f0All = estimateYinPitch(yTrimmed, sr)
        val validF0 = f0All.filter { it in 160.0..1950.0 }.toDoubleArray()
        if (validF0.size > 3) {
            val fMin = validF0.minOrNull() ?: 0.0
            val fMax = validF0.maxOrNull() ?: 0.0
            feats["f0_mean"] = validF0.average()
            feats["f0_std"] = std(validF0)
            feats["f0_min"] = fMin
            feats["f0_max"] = fMax
            feats["f0_range"] = fMax - fMin
            feats["f0_voiced_ratio"] = validF0.size.toDouble() / max(1, f0All.size)
            feats["f0_slope"] = linearSlope(validF0)
        } else {
            feats["f0_mean"] = 0.0
            feats["f0_std"] = 0.0
            feats["f0_min"] = 0.0
            feats["f0_max"] = 0.0
            feats["f0_range"] = 0.0
            feats["f0_voiced_ratio"] = 0.0
            feats["f0_slope"] = 0.0
        }

        // 5. Chroma 12 pitch classes
        val chroma = Array(12) { DoubleArray(nFrames) }
        for (t in 0 until nFrames) {
            var frameNorm = 0.0
            for (b in 1 until nBins) {
                val freq = freqs[b]
                if (freq in 65.0..2000.0) {
                    val midi = 69.0 + 12.0 * log2(freq / 440.0)
                    val pc = ((midi.roundToInt() % 12) + 12) % 12
                    chroma[pc][t] += powerSpec[b][t]
                }
            }
            for (pc in 0 until 12) frameNorm += chroma[pc][t]
            if (frameNorm > 1e-12) {
                for (pc in 0 until 12) chroma[pc][t] /= frameNorm
            }
        }
        for (i in 0 until 12) {
            feats["chroma_mean_$i"] = chroma[i].average()
            feats["chroma_std_$i"] = std(chroma[i])
        }

        // 6. Denoised Bioacoustic Dynamics
        val rmsClean = computeRmsFrames(yClean, frameLength = 256, hopLength = 128)
        var peakIdx = 0
        var peakVal = -1.0
        for (i in rmsClean.indices) {
            if (rmsClean[i] > peakVal) {
                peakVal = rmsClean[i]
                peakIdx = i
            }
        }
        feats["rms_peak_pos"] = peakIdx.toDouble() / max(1, rmsClean.size)
        feats["rms_decay_slope"] = if (peakIdx < rmsClean.size - 2) {
            linearSlope(rmsClean.copyOfRange(peakIdx, rmsClean.size))
        } else 0.0

        val cleanSpec = stftPowerSpectrogram(yClean, nFft = 512, hopLength = 128, center = true)
        val cleanFrames = cleanSpec[0].size
        val cleanBins = cleanSpec.size
        val cleanFreqs = DoubleArray(cleanBins) { it * (sr / 2.0) / (cleanBins - 1) }

        val flux = DoubleArray(cleanFrames)
        val ro25 = DoubleArray(cleanFrames)
        val ro50 = DoubleArray(cleanFrames)
        for (t in 0 until cleanFrames) {
            var totalE = 0.0
            var fSum = 0.0
            for (b in 0 until cleanBins) {
                val p = cleanSpec[b][t] + 1e-12
                totalE += p
                if (t > 0) {
                    val diffP = sqrt(cleanSpec[b][t]) - sqrt(cleanSpec[b][t - 1])
                    if (diffP > 0) fSum += diffP
                }
            }
            flux[t] = fSum / cleanBins
            var cum = 0.0
            var f25 = cleanFreqs.last()
            var f50 = cleanFreqs.last()
            var found25 = false
            var found50 = false
            for (b in 0 until cleanBins) {
                cum += cleanSpec[b][t] + 1e-12
                if (!found25 && cum >= 0.25 * totalE) { f25 = cleanFreqs[b]; found25 = true }
                if (!found50 && cum >= 0.50 * totalE) { f50 = cleanFreqs[b]; found50 = true }
            }
            ro25[t] = f25
            ro50[t] = f50
        }
        feats["flux_mean"] = flux.average()
        feats["flux_std"] = std(flux)
        feats["flux_max"] = flux.maxOrNull() ?: 0.0

        feats["rolloff_25_mean"] = ro25.average()
        feats["rolloff_25_std"] = std(ro25)
        feats["rolloff_50_mean"] = ro50.average()
        feats["rolloff_50_std"] = std(ro50)

        val f0CleanAll = estimateYinPitch(yClean, sr)
        val validCleanF0 = f0CleanAll.filter { it in 160.0..1950.0 }.toDoubleArray()
        if (validCleanF0.size > 4) {
            val sorted = validCleanF0.sortedArray()
            feats["f0_median"] = percentile(sorted, 50.0)
            feats["f0_iqr"] = percentile(sorted, 75.0) - percentile(sorted, 25.0)
            feats["f0_curvature"] = quadraticCurvature(validCleanF0)
            var diffSum = 0.0
            for (i in 1 until validCleanF0.size) {
                diffSum += abs(validCleanF0[i] - validCleanF0[i - 1])
            }
            val meanDiff = diffSum / (validCleanF0.size - 1)
            feats["f0_jitter"] = meanDiff / (validCleanF0.average() + 1e-6)
        } else {
            feats["f0_median"] = 0.0
            feats["f0_iqr"] = 0.0
            feats["f0_curvature"] = 0.0
            feats["f0_jitter"] = 0.0
        }

        return feats
    }

    private fun computeZcrFrames(y: FloatArray, frameLength: Int, hopLength: Int): DoubleArray {
        val nFrames = max(1, 1 + (y.size - frameLength) / hopLength)
        val out = DoubleArray(nFrames)
        for (f in 0 until nFrames) {
            val start = f * hopLength
            val end = min(y.size, start + frameLength)
            var crossings = 0
            for (i in start + 1 until end) {
                if ((y[i] >= 0f && y[i - 1] < 0f) || (y[i] < 0f && y[i - 1] >= 0f)) {
                    crossings++
                }
            }
            out[f] = crossings.toDouble() / max(1, end - start)
        }
        return out
    }

    private fun computeDelta(mat: Array<DoubleArray>): Array<DoubleArray> {
        val rows = mat.size
        val cols = mat[0].size
        val out = Array(rows) { DoubleArray(cols) }
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val prev = mat[r][max(0, c - 1)]
                val next = mat[r][min(cols - 1, c + 1)]
                out[r][c] = (next - prev) * 0.5
            }
        }
        return out
    }

    private fun std(arr: DoubleArray): Double {
        if (arr.isEmpty()) return 0.0
        val m = arr.average()
        var s = 0.0
        for (v in arr) s += (v - m) * (v - m)
        return sqrt(s / arr.size)
    }

    private fun skewness(arr: DoubleArray): Double {
        if (arr.size <= 3) return 0.0
        val m = arr.average()
        val s = std(arr)
        if (s < 1e-9) return 0.0
        var sum3 = 0.0
        for (v in arr) {
            val z = (v - m) / s
            sum3 += z * z * z
        }
        return sum3 / arr.size
    }

    private fun linearSlope(arr: DoubleArray): Double {
        if (arr.size < 2) return 0.0
        val n = arr.size.toDouble()
        val xm = (n - 1.0) / 2.0
        val ym = arr.average()
        var num = 0.0
        var den = 0.0
        for (i in arr.indices) {
            val dx = i - xm
            num += dx * (arr[i] - ym)
            den += dx * dx
        }
        return if (den > 1e-12) num / den else 0.0
    }

    private fun quadraticCurvature(arr: DoubleArray): Double {
        if (arr.size < 3) return 0.0
        // Simple 3-point normalized curvature estimate over [0, 1]
        val y0 = arr.first()
        val ym = arr[arr.size / 2]
        val y1 = arr.last()
        return 2.0 * (y0 - 2.0 * ym + y1)
    }

    private fun percentile(sorted: DoubleArray, p: Double): Double {
        if (sorted.isEmpty()) return 0.0
        val idx = (p / 100.0) * (sorted.size - 1)
        val lo = floor(idx).toInt()
        val hi = ceil(idx).toInt()
        val frac = idx - lo
        return sorted[lo] * (1.0 - frac) + sorted[hi] * frac
    }
}
