package com.example.ui.screens

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.ui.theme.Surface as MeowSurfaceColor
import kotlinx.coroutines.delay

@Composable
fun SplashVideoScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkSystem = isSystemInDarkTheme()
    val videoResId = if (isDarkSystem) R.raw.logo_motion_dark else R.raw.logo_motion_light
    // Exact sampled edge colors from the light/dark logo motion videos
    val backdropColor = if (isDarkSystem) Color(0xFF121716) else Color(0xFFE9E6D7)

    val context = LocalContext.current
    val currentOnFinished by rememberUpdatedState(onFinished)

    var videoAspectRatio by remember { mutableFloatStateOf(9f / 16f) }
    var firstFrameRendered by remember { mutableStateOf(false) }
    var isFadingOut by remember { mutableStateOf(false) }
    var hasCompleted by remember { mutableStateOf(false) }

    val startCoverAlpha by animateFloatAsState(
        targetValue = if (firstFrameRendered) 0f else 1f,
        animationSpec = tween(durationMillis = 80),
        label = "splash_start_cover"
    )

    val exitScrimAlpha by animateFloatAsState(
        targetValue = if (isFadingOut) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "splash_exit_scrim"
    )

    fun triggerSmoothExit() {
        if (!isFadingOut) {
            isFadingOut = true
        }
    }

    LaunchedEffect(isFadingOut) {
        if (isFadingOut && !hasCompleted) {
            delay(230L)
            hasCompleted = true
            currentOnFinished()
        }
    }

    // Safety fallback in case video codec fails or stalls on an unusual device
    LaunchedEffect(videoResId) {
        delay(6000L)
        triggerSmoothExit()
    }

    val mediaPlayerHolder = remember { arrayOfNulls<MediaPlayer>(1) }

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
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backdropColor)
            .clipToBounds()
            .testTag("splash_video_screen"),
        contentAlignment = Alignment.Center
    ) {
        val containerRatio = if (maxHeight > 0.dp) maxWidth / maxHeight else videoAspectRatio
        val safeVideoRatio = videoAspectRatio.coerceIn(0.25f, 4f)

        // Preserve exact 1:1 pixel geometry so the logo circle is always perfectly round:
        // - On tall handheld screens (aspect <= 0.72), fill height and keep exact 9:16 width
        //   (edge-to-edge full screen with generous margin around the centered circle).
        // - On wide screens/tablets (aspect > 0.72), fit cleanly inside height so the circle is never clipped.
        val (surfaceWidth, surfaceHeight) = if (containerRatio > 0.72f) {
            val h = maxHeight
            val w = h * safeVideoRatio
            if (w <= maxWidth) w to h else maxWidth to (maxWidth / safeVideoRatio)
        } else if (containerRatio > safeVideoRatio) {
            maxWidth to (maxWidth / safeVideoRatio)
        } else {
            (maxHeight * safeVideoRatio) to maxHeight
        }

        // SurfaceView sized to the video's exact aspect ratio renders 60fps H.264 directly
        // to the Hardware Composer with zero stretching and zero GPU alpha-blending overhead.
        AndroidView(
            modifier = Modifier.requiredSize(width = surfaceWidth, height = surfaceHeight),
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
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
                                    player.setDataSource(
                                        afd.fileDescriptor,
                                        afd.startOffset,
                                        afd.length
                                    )
                                }
                                player.setDisplay(holder)
                                player.setVideoScalingMode(
                                    MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT
                                )
                                player.isLooping = false

                                player.setOnVideoSizeChangedListener { _, videoW, videoH ->
                                    if (videoW > 0 && videoH > 0) {
                                        videoAspectRatio = videoW.toFloat() / videoH.toFloat()
                                    }
                                }

                                player.setOnInfoListener { _, what, _ ->
                                    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                                        firstFrameRendered = true
                                    }
                                    false
                                }

                                player.setOnPreparedListener { mp ->
                                    if (mp.videoWidth > 0 && mp.videoHeight > 0) {
                                        videoAspectRatio = mp.videoWidth.toFloat() / mp.videoHeight.toFloat()
                                    }
                                    mp.start()
                                    // Fallback in case a device codec does not emit VIDEO_RENDERING_START
                                    postDelayed({ firstFrameRendered = true }, 140L)
                                }

                                player.setOnCompletionListener {
                                    triggerSmoothExit()
                                }

                                player.setOnErrorListener { _, _, _ ->
                                    triggerSmoothExit()
                                    true
                                }

                                player.prepareAsync()
                            } catch (_: Exception) {
                                triggerSmoothExit()
                            }
                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder,
                            format: Int,
                            width: Int,
                            height: Int
                        ) = Unit

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            try {
                                mediaPlayerHolder[0]?.stop()
                                mediaPlayerHolder[0]?.release()
                            } catch (_: Exception) {
                            }
                            mediaPlayerHolder[0] = null
                        }
                    })
                }
            }
        )

        // Seamless initial cover hides SurfaceView until Frame 0 is rendered
        if (startCoverAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backdropColor.copy(alpha = startCoverAlpha))
            )
        }

        // Smooth dissolve into the app's warm surface color when motion ends
        if (exitScrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MeowSurfaceColor.copy(alpha = exitScrimAlpha))
            )
        }
    }
}
