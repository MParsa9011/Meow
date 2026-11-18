package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.model.BundledSample
import com.example.data.model.IntentCode
import com.example.data.model.IntentMatch
import com.example.data.model.PetProfile
import com.example.ui.RecordingPhase
import com.example.ui.TranslatorUiState
import com.example.ui.components.CatAvatarImage
import com.example.ui.theme.AffectionCoral
import com.example.ui.theme.AffectionCoralContainer
import com.example.ui.theme.ConfidenceEmerald
import com.example.ui.theme.ConfidenceEmeraldBg
import com.example.ui.theme.DistressPeriwinkle
import com.example.ui.theme.DistressPeriwinkleContainer
import com.example.ui.theme.FoodAmber
import com.example.ui.theme.FoodAmberContainer
import com.example.ui.theme.LiveRecordingCrimson
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.OnPrimaryContainer
import com.example.ui.theme.OnPrimaryFixed
import com.example.ui.theme.OnSurface
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.OutlineVariant
import com.example.ui.theme.Primary
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.PrimaryFixed
import com.example.ui.theme.PrimaryFixedDim
import com.example.ui.theme.SecondaryFixed
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.Tertiary
import com.example.ui.theme.TertiaryFixed
import com.example.ui.theme.VazirmatnFamily

@Composable
fun TranslateScreen(
    activePet: PetProfile?,
    uiState: TranslatorUiState,
    settings: AppSettings,
    liveAmplitudes: List<Float>,
    playingAudioId: String?,
    bundledSamples: List<BundledSample>,
    hasMicPermission: Boolean,
    onToggleRecording: () -> Unit,
    onUploadAudio: () -> Unit,
    onSelectSample: (BundledSample) -> Unit,
    onSelectMatchRank: (Int) -> Unit,
    onPlayCatResponse: (String, IntentCode) -> Unit,
    onPlayAudioClip: (String, String?, String?) -> Unit,
    onToggleBookmark: () -> Unit,
    onShareResult: (IntentMatch) -> Unit,
    onSubmitFeedback: (String, IntentCode?) -> Unit,
    onOpenSwitchPet: () -> Unit,
    onOpenModelSelector: () -> Unit,
    modifier: Modifier = Modifier
) {
    var feedbackVerdict by remember(uiState.analysisResult) { mutableStateOf<String?>(null) }
    var correctedIntent by remember(uiState.analysisResult) { mutableStateOf<IntentCode?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("translate_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            ActivePetStatusCard(
                pet = activePet,
                onSwitchClick = onOpenSwitchPet,
                modifier = Modifier.widthIn(max = 600.dp)
            )
        }

        item {
            VoiceRecordingHub(
                uiState = uiState,
                settings = settings,
                liveAmplitudes = liveAmplitudes,
                bundledSamples = bundledSamples,
                hasMicPermission = hasMicPermission,
                onToggleRecording = onToggleRecording,
                onUploadAudio = onUploadAudio,
                onSelectSample = onSelectSample,
                onOpenModelSelector = onOpenModelSelector,
                modifier = Modifier.widthIn(max = 600.dp)
            )
        }

        val analysis = uiState.analysisResult
        if (analysis != null) {
            item {
                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Psychology,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.ai_voice_analysis),
                                style = MaterialTheme.typography.titleLarge,
                                color = OnSurface
                            )
                        }
                        Surface(
                            color = ConfidenceEmeraldBg,
                            shape = CircleShape
                        ) {
                            Text(
                                text = "${analysis.confidence}% Confident",
                                style = MaterialTheme.typography.labelMedium,
                                color = ConfidenceEmerald,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    analysis.matches.forEach { match ->
                        IntentMatchAccordionCard(
                            match = match,
                            isExpanded = uiState.expandedMatchRank == match.rank,
                            showPersian = settings.showPersianSubtitles,
                            isPlayingCatResponse = playingAudioId == "cat_resp_${match.rank}",
                            isPlayingRecording = playingAudioId == "rec_clip_${match.rank}",
                            hasRecordingAudio = analysis.audioFilePath != null || analysis.sampleAssetName != null,
                            isBookmarked = uiState.isCurrentBookmarked,
                            onExpandToggle = {
                                onSelectMatchRank(if (uiState.expandedMatchRank == match.rank) -1 else match.rank)
                            },
                            onPlayCatResponse = {
                                onPlayCatResponse("cat_resp_${match.rank}", match.intentCode)
                            },
                            onPlayRecording = {
                                onPlayAudioClip(
                                    "rec_clip_${match.rank}",
                                    analysis.audioFilePath,
                                    analysis.sampleAssetName
                                )
                            },
                            onToggleBookmark = onToggleBookmark,
                            onShare = { onShareResult(match) }
                        )
                    }
                }
            }

            item {
                ActiveLearningFeedbackCard(
                    feedbackSubmitted = uiState.feedbackSubmitted,
                    selectedVerdict = feedbackVerdict,
                    selectedCorrection = correctedIntent,
                    onSelectVerdict = { verdict ->
                        feedbackVerdict = verdict
                        if (verdict == "Correct") {
                            correctedIntent = null
                        }
                    },
                    onSelectCorrection = { correctedIntent = it },
                    onSubmit = {
                        val v = feedbackVerdict ?: return@ActiveLearningFeedbackCard
                        onSubmitFeedback(v, correctedIntent)
                    },
                    modifier = Modifier.widthIn(max = 600.dp)
                )
            }
        }

        item {
            DailyBehaviorTipCard(modifier = Modifier.widthIn(max = 600.dp))
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ActivePetStatusCard(
    pet: PetProfile?,
    onSwitchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    CatAvatarImage(pet = pet, size = 50.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = pet?.name ?: "Lucy",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            Surface(
                                color = PrimaryFixed,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnPrimaryFixed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${pet?.breed ?: "Ginger Tabby"} • ${pet?.ageDisplay ?: "2 yrs 4 mos"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OutlinedButton(
                    onClick = onSwitchClick,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, OutlineVariant),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("home_switch_pet_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SwapHoriz,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Switch",
                        style = MaterialTheme.typography.labelLarge,
                        color = Primary
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Acoustic Profile Calibration",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant
                    )
                    Text(
                        text = "${pet?.calibratedPct ?: 100}% • ${pet?.baselinePitchHz ?: 420} Hz",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }
                LinearProgressIndicator(
                    progress = { ((pet?.calibratedPct ?: 100) / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(CircleShape),
                    color = PrimaryContainer,
                    trackColor = SurfaceContainerHigh,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun VoiceRecordingHub(
    uiState: TranslatorUiState,
    settings: AppSettings,
    liveAmplitudes: List<Float>,
    bundledSamples: List<BundledSample>,
    hasMicPermission: Boolean,
    onToggleRecording: () -> Unit,
    onUploadAudio: () -> Unit,
    onSelectSample: (BundledSample) -> Unit,
    onOpenModelSelector: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecording = uiState.phase == RecordingPhase.RECORDING
    val isAnalyzing = uiState.phase == RecordingPhase.ANALYZING

    val infiniteTransition = rememberInfiniteTransition(label = "record_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.18f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isRecording) 650 else 1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val buttonColor by animateColorAsState(
        targetValue = if (isRecording) LiveRecordingCrimson else PrimaryContainer,
        label = "record_btn_color"
    )

    val waveformValues = when {
        isRecording -> liveAmplitudes
        uiState.analysisResult != null -> uiState.analysisResult.waveformBars
        else -> liveAmplitudes
    }

    Surface(
        color = SurfaceContainerLowest,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f)),
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isRecording) LiveRecordingCrimson.copy(alpha = 0.14f) else SurfaceContainer,
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isRecording) LiveRecordingCrimson else PrimaryContainer
                                )
                        )
                        Text(
                            text = uiState.statusText,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isRecording) LiveRecordingCrimson else OnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    color = TertiaryFixed,
                    shape = CircleShape,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onOpenModelSelector)
                        .testTag("home_model_chip")
                ) {
                    Text(
                        text = when (settings.modelEngine.id) {
                            "hybrid" -> "Hybrid AI • 79.4%"
                            "acoustic" -> "Bioacoustic • 74.6%"
                            else -> "Mel CNN • 76.2%"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Tertiary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Live Waveform Visualizer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                waveformValues.take(28).forEachIndexed { idx, amp ->
                    val targetHeight = (10f + amp * 50f).coerceIn(10f, 60f)
                    val animatedHeight by animateFloatAsState(
                        targetValue = targetHeight,
                        animationSpec = tween(140),
                        label = "bar_$idx"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(animatedHeight.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isRecording -> LiveRecordingCrimson
                                    uiState.analysisResult != null -> PrimaryContainer.copy(
                                        alpha = 0.45f + (amp * 0.55f).coerceIn(0f, 0.55f)
                                    )
                                    else -> OutlineVariant.copy(alpha = 0.6f)
                                }
                            )
                    )
                }
            }

            // Record Button with Pulsing Ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(116.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(98.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            if (isRecording) LiveRecordingCrimson.copy(alpha = 0.20f)
                            else PrimaryFixed.copy(alpha = 0.55f)
                        )
                )
                Surface(
                    onClick = onToggleRecording,
                    enabled = !isAnalyzing,
                    shape = CircleShape,
                    color = buttonColor,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(80.dp)
                        .testTag("record_meow_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                color = OnPrimary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(30.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                                contentDescription = if (isRecording) "Stop recording" else "Record cat meow",
                                tint = OnPrimary,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = when {
                        isRecording -> "Listening... Tap stop to translate"
                        isAnalyzing -> "Extracting 234 bioacoustic & Mel-CNN features..."
                        !hasMicPermission -> "Tap microphone to grant access & record"
                        else -> stringResource(R.string.tap_to_record)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.recording_distance_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Upload Audio Button
            OutlinedButton(
                onClick = onUploadAudio,
                enabled = !isRecording && !isAnalyzing,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, OutlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("upload_audio_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.AudioFile,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.upload_audio),
                    style = MaterialTheme.typography.labelLarge,
                    color = Primary
                )
            }

            // Real Dataset Sample Quick-Test Strip
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Or test with real cat vocalization samples",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    bundledSamples.forEach { sample ->
                        val selected = uiState.activeSampleId == sample.id
                        Surface(
                            onClick = { onSelectSample(sample) },
                            enabled = !isRecording && !isAnalyzing,
                            color = if (selected) PrimaryFixed else SurfaceContainerLow,
                            shape = CircleShape,
                            border = BorderStroke(
                                1.dp,
                                if (selected) PrimaryContainer else OutlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("sample_pill_${sample.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.GraphicEq,
                                    contentDescription = null,
                                    tint = if (selected) Primary else OnSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = sample.shortLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) OnPrimaryFixed else OnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntentMatchAccordionCard(
    match: IntentMatch,
    isExpanded: Boolean,
    showPersian: Boolean,
    isPlayingCatResponse: Boolean,
    isPlayingRecording: Boolean,
    hasRecordingAudio: Boolean,
    isBookmarked: Boolean,
    onExpandToggle: () -> Unit,
    onPlayCatResponse: () -> Unit,
    onPlayRecording: () -> Unit,
    onToggleBookmark: () -> Unit,
    onShare: () -> Unit
) {
    val (accentColor, containerColor, iconVector) = when (match.intentCode) {
        IntentCode.FOOD -> Triple(FoodAmber, FoodAmberContainer, Icons.Filled.Restaurant)
        IntentCode.BRUSHING -> Triple(AffectionCoral, AffectionCoralContainer, Icons.Filled.Favorite)
        IntentCode.ISOLATION -> Triple(DistressPeriwinkle, DistressPeriwinkleContainer, Icons.Filled.WarningAmber)
    }

    val isTop = match.rank == 1

    Surface(
        color = if (isTop) SurfaceContainerLowest else SurfaceContainerLow,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            width = if (isTop) 1.5.dp else 1.dp,
            color = if (isTop) PrimaryContainer.copy(alpha = 0.65f) else OutlineVariant.copy(alpha = 0.35f)
        ),
        shadowElevation = if (isTop) 3.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("intent_match_card_${match.rank}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpandToggle)
                    .minimumInteractiveComponentSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = match.title,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = if (isTop) PrimaryFixed else SurfaceContainerHigh,
                            shape = CircleShape
                        ) {
                            Text(
                                text = match.badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isTop) OnPrimaryFixed else OnSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "${match.percentage}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isTop) Primary else OnSurface
                        )
                    }
                    Text(
                        text = match.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    if (showPersian) {
                        Text(
                            text = match.titleFa,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = VazirmatnFamily,
                                fontSize = 12.sp
                            ),
                            color = OnSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse details" else "Expand details",
                    tint = OnSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { (match.percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (isTop) PrimaryContainer else accentColor.copy(alpha = 0.7f),
                trackColor = SurfaceContainerHigh,
                strokeCap = StrokeCap.Round
            )

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = match.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                    if (showPersian) {
                        Surface(
                            color = SurfaceContainerLow,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = match.descriptionFa,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = VazirmatnFamily,
                                        lineHeight = 19.sp
                                    ),
                                    color = OnSurface
                                )
                                Text(
                                    text = match.careTipFa,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = VazirmatnFamily
                                    ),
                                    color = Primary
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AcousticMiniPill(
                            label = "Pitch F0",
                            value = match.pitchHz,
                            modifier = Modifier.weight(1f)
                        )
                        AcousticMiniPill(
                            label = "Duration",
                            value = match.durationSec,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = OutlineVariant.copy(alpha = 0.35f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onPlayCatResponse,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPlayingCatResponse) Tertiary else PrimaryContainer,
                                contentColor = OnPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("play_cat_response_button_${match.rank}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPlayingCatResponse) "Playing Purr..." else "Play Cat Response",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (hasRecordingAudio) {
                            IconButton(
                                onClick = onPlayRecording,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(CircleShape)
                                    .background(if (isPlayingRecording) PrimaryFixed else SurfaceContainer)
                                    .testTag("play_recorded_audio_button_${match.rank}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.GraphicEq,
                                    contentDescription = "Play analyzed audio",
                                    tint = if (isPlayingRecording) Primary else OnSurfaceVariant
                                )
                            }
                        }

                        if (isTop) {
                            IconButton(
                                onClick = onToggleBookmark,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(CircleShape)
                                    .background(SurfaceContainer)
                                    .testTag("bookmark_analysis_button")
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = if (isBookmarked) "Remove bookmark" else "Save to history",
                                    tint = if (isBookmarked) Primary else OnSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onShare,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .background(SurfaceContainer)
                                .testTag("share_analysis_button_${match.rank}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share translation",
                                tint = OnSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AcousticMiniPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )
        }
    }
}

@Composable
private fun ActiveLearningFeedbackCard(
    feedbackSubmitted: Boolean,
    selectedVerdict: String?,
    selectedCorrection: IntentCode?,
    onSelectVerdict: (String) -> Unit,
    onSelectCorrection: (IntentCode) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceContainerLow,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Pets,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.feedback_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
            }

            if (feedbackSubmitted) {
                Surface(
                    color = ConfidenceEmeraldBg,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = ConfidenceEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Thanks! Active learning log updated for future calibration.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ConfidenceEmerald
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val yesSelected = selectedVerdict == "Correct"
                    val noSelected = selectedVerdict == "Wrong"

                    OutlinedButton(
                        onClick = { onSelectVerdict("Correct") },
                        shape = CircleShape,
                        border = BorderStroke(
                            1.dp,
                            if (yesSelected) ConfidenceEmerald else OutlineVariant
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (yesSelected) ConfidenceEmeraldBg else Color.Transparent,
                            contentColor = if (yesSelected) ConfidenceEmerald else OnSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("feedback_yes_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ThumbUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.feedback_yes))
                    }

                    OutlinedButton(
                        onClick = { onSelectVerdict("Wrong") },
                        shape = CircleShape,
                        border = BorderStroke(
                            1.dp,
                            if (noSelected) PrimaryContainer else OutlineVariant
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (noSelected) PrimaryFixed else Color.Transparent,
                            contentColor = if (noSelected) OnPrimaryFixed else OnSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("feedback_no_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ThumbDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.feedback_no))
                    }
                }

                AnimatedVisibility(visible = selectedVerdict == "Wrong") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.feedback_correct_prompt),
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                IntentCode.BRUSHING to "Brushing / Attention",
                                IntentCode.FOOD to "Waiting for Food",
                                IntentCode.ISOLATION to "Isolation / Distress"
                            ).forEach { (code, label) ->
                                val picked = selectedCorrection == code
                                Surface(
                                    onClick = { onSelectCorrection(code) },
                                    color = if (picked) PrimaryFixed else SurfaceContainerLowest,
                                    shape = CircleShape,
                                    border = BorderStroke(
                                        1.dp,
                                        if (picked) PrimaryContainer else OutlineVariant
                                    ),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("feedback_correct_${code.code}")
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (picked) OnPrimaryFixed else OnSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                val canSubmit = selectedVerdict == "Correct" ||
                    (selectedVerdict == "Wrong" && selectedCorrection != null)

                Button(
                    onClick = onSubmit,
                    enabled = canSubmit,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryContainer,
                        contentColor = OnPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("feedback_submit_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.feedback_submit))
                }
            }
        }
    }
}

@Composable
private fun DailyBehaviorTipCard(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = SecondaryFixed,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            PrimaryFixed.copy(alpha = 0.85f),
                            SecondaryFixed.copy(alpha = 0.9f)
                        )
                    )
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(SurfaceContainerLowest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = PrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.daily_tip_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnPrimaryFixed
                )
                Text(
                    text = stringResource(R.string.daily_tip_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnPrimaryContainer
                )
            }
        }
    }
}
