package com.example.ui.screens

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun SplashVideoScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkSystem = isSystemInDarkTheme()
    val videoResId = if (isDarkSystem) R.raw.logo_motion_dark else R.raw.logo_motion_light
    val backdropColor = if (isDarkSystem) Color(0xFF0E0E0E) else Color(0xFFF3EFE6)

    val context = LocalContext.current
    val currentOnFinished by rememberUpdatedState(onFinished)
    var hasCompleted by remember { mutableStateOf(false) }

    fun finishOnce() {
        if (!hasCompleted) {
            hasCompleted = true
            currentOnFinished()
        }
    }

    // Safety fallback in case video codec fails or stalls on an unusual device
    LaunchedEffect(videoResId) {
        delay(6500L)
        finishOnce()
    }

    val mediaPlayerHolder = remember { arrayOfNulls<MediaPlayer>(1) }
    val surfaceHolder = remember { arrayOfNulls<Surface>(1) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayerHolder[0]?.stop()
            } catch (_: Exception) {
            }
            try {
                mediaPlayerHolder[0]?.release()
            } catch (_: Exception) {
            }
            mediaPlayerHolder[0] = null
            surfaceHolder[0]?.release()
            surfaceHolder[0] = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backdropColor)
            .testTag("splash_video_screen")
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                TextureView(ctx).apply {
                    isOpaque = false
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            st: SurfaceTexture,
                            viewWidth: Int,
                            viewHeight: Int
                        ) {
                            val surface = Surface(st)
                            surfaceHolder[0] = surface
                            try {
                                val player = MediaPlayer()
                                mediaPlayerHolder[0] = player
                                player.setAudioAttributes(
                                    AudioAttributes.Builder()
                                        .setUsage(AudioAttributes.USAGE_MEDIA)
                                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                                        .build()
                                )
                                context.resources.openRawResourceFd(videoResId).use { afd ->
                                    player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                }
                                player.setSurface(surface)
                                player.isLooping = false

                                player.setOnVideoSizeChangedListener { _, videoW, videoH ->
                                    if (videoW > 0 && videoH > 0 && width > 0 && height > 0) {
                                        applyCenterCropTransform(this@apply, width, height, videoW, videoH)
                                    }
                                }

                                player.setOnPreparedListener { mp ->
                                    if (mp.videoWidth > 0 && mp.videoHeight > 0 && width > 0 && height > 0) {
                                        applyCenterCropTransform(
                                            this@apply,
                                            width,
                                            height,
                                            mp.videoWidth,
                                            mp.videoHeight
                                        )
                                    }
                                    mp.start()
                                }

                                player.setOnCompletionListener {
                                    finishOnce()
                                }

                                player.setOnErrorListener { _, _, _ ->
                                    finishOnce()
                                    true
                                }

                                player.prepareAsync()
                            } catch (_: Exception) {
                                finishOnce()
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(
                            st: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) {
                            val mp = mediaPlayerHolder[0] ?: return
                            if (mp.videoWidth > 0 && mp.videoHeight > 0 && width > 0 && height > 0) {
                                applyCenterCropTransform(this@apply, width, height, mp.videoWidth, mp.videoHeight)
                            }
                        }

                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                            try {
                                mediaPlayerHolder[0]?.stop()
                                mediaPlayerHolder[0]?.release()
                            } catch (_: Exception) {
                            }
                            mediaPlayerHolder[0] = null
                            surfaceHolder[0]?.release()
                            surfaceHolder[0] = null
                            return true
                        }

                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
                    }
                }
            }
        )
    }
}

private fun applyCenterCropTransform(
    textureView: TextureView,
    viewWidth: Int,
    viewHeight: Int,
    videoWidth: Int,
    videoHeight: Int
) {
    val viewRatio = viewWidth.toFloat() / viewHeight.toFloat()
    val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()

    var scaleX = 1f
    var scaleY = 1f
    if (videoRatio > viewRatio) {
        scaleX = videoRatio / viewRatio
    } else {
        scaleY = viewRatio / videoRatio
    }

    val pivotX = viewWidth / 2f
    val pivotY = viewHeight / 2f
    val matrix = Matrix().apply {
        setScale(scaleX, scaleY, pivotX, pivotY)
    }
    textureView.setTransform(matrix)
}
