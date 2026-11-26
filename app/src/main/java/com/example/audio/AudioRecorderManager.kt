package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import com.example.data.model.IntentCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class AudioRecorderManager(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var recordedSamples = mutableListOf<Short>()
    private var activeSampleRate = 16000

    private val _liveAmplitudes = MutableStateFlow(List(12) { 0.18f })
    val liveAmplitudes: StateFlow<List<Float>> = _liveAmplitudes.asStateFlow()

    private val _playingId = MutableStateFlow<String?>(null)
    val playingId: StateFlow<String?> = _playingId.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    @SuppressLint("MissingPermission")
    fun startRecording(sampleRate: Int = 16000) {
        stopRecordingInternal()
        activeSampleRate = sampleRate
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuf * 2
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw IllegalStateException("AudioRecord could not initialize microphone")
        }

        recordedSamples = mutableListOf()
        audioRecord = record
        record.startRecording()

        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            val buf = ShortArray(1024)
            while (isActive && record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val read = record.read(buf, 0, buf.size)
                if (read > 0) {
                    synchronized(recordedSamples) {
                        for (i in 0 until read) recordedSamples.add(buf[i])
                    }
                    var sumSq = 0.0
                    for (i in 0 until read) {
                        val norm = buf[i] / 32768.0
                        sumSq += norm * norm
                    }
                    val rms = sqrt(sumSq / read).toFloat()
                    val scaled = (rms * 5.5f).coerceIn(0.15f, 1.0f)
                    val prev = _liveAmplitudes.value
                    _liveAmplitudes.value = prev.drop(1) + scaled
                }
            }
        }
    }

    suspend fun stopAndGetAudio(saveLocally: Boolean): Pair<FloatArray, String?> = withContext(Dispatchers.IO) {
        stopRecordingInternal()
        val pcm: ShortArray
        synchronized(recordedSamples) {
            pcm = recordedSamples.toShortArray()
            recordedSamples.clear()
        }
        val floatRaw = FloatArray(pcm.size) { pcm[it] / 32768f }
        val resampled8k = AudioDsp.resampleLinear(floatRaw, activeSampleRate, AudioDsp.TARGET_SR)

        val savedPath = if (saveLocally && pcm.isNotEmpty()) {
            val dir = File(context.filesDir, "recordings").apply { mkdirs() }
            val wavFile = File(dir, "meow_${System.currentTimeMillis()}.wav")
            writeWavFile(wavFile, pcm, activeSampleRate)
            wavFile.absolutePath
        } else null

        resampled8k to savedPath
    }

    private fun stopRecordingInternal() {
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) stop()
                release()
            }
        } catch (_: Exception) {
        }
        audioRecord = null
    }

    suspend fun loadBundledSample(assetFilename: String): FloatArray = withContext(Dispatchers.IO) {
        context.assets.open("samples/$assetFilename").use { input ->
            decodeWavStream(input.readBytes())
        }
    }

    suspend fun decodeAudioUri(uri: Uri, saveLocally: Boolean): Pair<FloatArray, String?> = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Unable to read selected audio file")

        val isWav = bytes.size >= 12 &&
            String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" &&
            String(bytes, 8, 4, Charsets.US_ASCII) == "WAVE"

        val samples8k = if (isWav) {
            decodeWavStream(bytes)
        } else {
            decodeWithMediaCodec(uri)
        }

        val savedPath = if (saveLocally && samples8k.isNotEmpty()) {
            val dir = File(context.filesDir, "recordings").apply { mkdirs() }
            val wavFile = File(dir, "upload_${System.currentTimeMillis()}.wav")
            val pcm16 = ShortArray(samples8k.size) { (samples8k[it].coerceIn(-1f, 1f) * 32767f).toInt().toShort() }
            writeWavFile(wavFile, pcm16, AudioDsp.TARGET_SR)
            wavFile.absolutePath
        } else null

        samples8k to savedPath
    }

    private fun decodeWavStream(bytes: ByteArray): FloatArray {
        if (bytes.size < 44) return FloatArray(0)
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var pos = 12
        var channels = 1
        var sampleRate = AudioDsp.TARGET_SR
        var bitsPerSample = 16
        var audioFormat = 1
        var dataStart = -1
        var dataSize = 0

        while (pos + 8 <= bytes.size) {
            val chunkId = String(bytes, pos, 4, Charsets.US_ASCII)
            val chunkSize = bb.getInt(pos + 4)
            pos += 8
            if (chunkId == "fmt " && pos + 16 <= bytes.size) {
                audioFormat = bb.getShort(pos).toInt() and 0xFFFF
                channels = bb.getShort(pos + 2).toInt() and 0xFFFF
                sampleRate = bb.getInt(pos + 4)
                bitsPerSample = bb.getShort(pos + 14).toInt() and 0xFFFF
            } else if (chunkId == "data") {
                dataStart = pos
                dataSize = min(chunkSize, bytes.size - pos)
                break
            }
            pos += chunkSize + (chunkSize and 1)
        }

        if (dataStart < 0 || dataSize <= 0) return FloatArray(0)
        val bytesPerSample = max(1, bitsPerSample / 8)
        val frameSize = bytesPerSample * max(1, channels)
        val totalFrames = dataSize / frameSize
        val mono = FloatArray(totalFrames)

        for (i in 0 until totalFrames) {
            var chSum = 0f
            for (c in 0 until channels) {
                val off = dataStart + i * frameSize + c * bytesPerSample
                val sample = when {
                    audioFormat == 3 && bitsPerSample == 32 && off + 4 <= bytes.size ->
                        bb.getFloat(off)
                    bitsPerSample == 16 && off + 2 <= bytes.size ->
                        bb.getShort(off) / 32768f
                    bitsPerSample == 24 && off + 3 <= bytes.size -> {
                        val b0 = bytes[off].toInt() and 0xFF
                        val b1 = bytes[off + 1].toInt() and 0xFF
                        val b2 = bytes[off + 2].toInt()
                        ((b2 shl 16) or (b1 shl 8) or b0) / 8388608f
                    }
                    bitsPerSample == 8 && off < bytes.size ->
                        ((bytes[off].toInt() and 0xFF) - 128) / 128f
                    else -> 0f
                }
                chSum += sample
            }
            mono[i] = chSum / max(1, channels)
        }
        return AudioDsp.resampleLinear(mono, sampleRate, AudioDsp.TARGET_SR)
    }

    private fun decodeWithMediaCodec(uri: Uri): FloatArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)
        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                trackIndex = i
                format = f
                break
            }
        }
        if (trackIndex < 0 || format == null) {
            extractor.release()
            throw IllegalArgumentException("No audio track found in selected file")
        }

        extractor.selectTrack(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME)!!
        var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val pcmOut = ByteArrayOutputStream()
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false

        try {
            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(5000)
                    if (inIdx >= 0) {
                        val buf = codec.getInputBuffer(inIdx)!!
                        val size = extractor.readSampleData(buf, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIdx = codec.dequeueOutputBuffer(info, 5000)
                if (outIdx >= 0) {
                    val outBuf = codec.getOutputBuffer(outIdx)!!
                    val chunk = ByteArray(info.size)
                    outBuf.get(chunk)
                    outBuf.clear()
                    pcmOut.write(chunk)
                    codec.releaseOutputBuffer(outIdx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFmt = codec.outputFormat
                    if (newFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRate = newFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (newFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channels = newFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                }
            }
        } finally {
            codec.stop()
            codec.release()
            extractor.release()
        }

        val rawBytes = pcmOut.toByteArray()
        val bb = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN)
        val totalShorts = rawBytes.size / 2
        val frames = totalShorts / max(1, channels)
        val mono = FloatArray(frames)
        for (i in 0 until frames) {
            var sum = 0f
            for (c in 0 until channels) {
                sum += bb.getShort((i * channels + c) * 2) / 32768f
            }
            mono[i] = sum / max(1, channels)
        }
        return AudioDsp.resampleLinear(mono, sampleRate, AudioDsp.TARGET_SR)
    }

    fun playAudioSource(playId: String, filePath: String?, sampleAssetName: String?) {
        if (_playingId.value == playId) {
            stopPlayback()
            return
        }
        stopPlayback()

        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
            }
            if (filePath != null && File(filePath).exists()) {
                mp.setDataSource(filePath)
            } else if (sampleAssetName != null) {
                try {
                    val afd = context.assets.openFd("samples/$sampleAssetName")
                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                } catch (_: Exception) {
                    // Fallback if asset is compressed in APK: cache locally for MediaPlayer
                    val cacheFile = File(context.cacheDir, "sample_$sampleAssetName")
                    if (!cacheFile.exists()) {
                        context.assets.open("samples/$sampleAssetName").use { input ->
                            FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
                        }
                    }
                    mp.setDataSource(cacheFile.absolutePath)
                }
            } else {
                mp.release()
                return
            }
            mp.setOnCompletionListener {
                if (_playingId.value == playId) {
                    _playingId.value = null
                }
                it.release()
                if (mediaPlayer === it) mediaPlayer = null
            }
            mp.setOnErrorListener { player, _, _ ->
                if (_playingId.value == playId) {
                    _playingId.value = null
                }
                player.release()
                if (mediaPlayer === player) mediaPlayer = null
                true
            }
            mp.prepare()
            mp.start()
            mediaPlayer = mp
            _playingId.value = playId
        } catch (_: Exception) {
            _playingId.value = null
        }
    }

    /**
     * Plays a real recorded feline vocalization matching the predicted intent
     * when the user taps "Play Cat Response".
     */
    fun playCatResponse(playId: String, intentCode: IntentCode) {
        val realCatSample = when (intentCode) {
            IntentCode.FOOD -> "F_MIN01_EU_FN_BEN01_102.wav"
            IntentCode.BRUSHING -> "B_BRA01_MC_MN_SIM01_301.wav"
            IntentCode.ISOLATION -> "I_MIN01_EU_FN_BEN01_106.wav"
        }
        playAudioSource(playId = playId, filePath = null, sampleAssetName = realCatSample)
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        _playingId.value = null
    }

    private fun writeWavFile(file: File, pcm: ShortArray, sampleRate: Int) {
        val dataSize = pcm.size * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + dataSize)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // Subchunk1Size
            putShort(1.toShort()) // PCM
            putShort(1.toShort()) // Mono
            putInt(sampleRate)
            putInt(sampleRate * 2)
            putShort(2.toShort())
            putShort(16.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
        }
        FileOutputStream(file).use { fos ->
            fos.write(header.array())
            val dataBuf = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
            for (s in pcm) dataBuf.putShort(s)
            fos.write(dataBuf.array())
        }
    }
}
