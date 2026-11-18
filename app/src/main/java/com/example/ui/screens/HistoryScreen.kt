package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IntentCode
import com.example.data.model.TranslationRecord
import com.example.ui.theme.AffectionCoral
import com.example.ui.theme.AffectionCoralContainer
import com.example.ui.theme.ConfidenceEmerald
import com.example.ui.theme.ConfidenceEmeraldBg
import com.example.ui.theme.DistressPeriwinkle
import com.example.ui.theme.DistressPeriwinkleContainer
import com.example.ui.theme.FoodAmber
import com.example.ui.theme.FoodAmberContainer
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.OnPrimaryContainer
import com.example.ui.theme.OnPrimaryFixed
import com.example.ui.theme.OnSurface
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.OutlineVariant
import com.example.ui.theme.Primary
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.PrimaryFixed
import com.example.ui.theme.SecondaryFixed
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.VazirmatnFamily

@Composable
fun HistoryScreen(
    translations: List<TranslationRecord>,
    playingAudioId: String?,
    showPersian: Boolean,
    onPlayAudio: (TranslationRecord) -> Unit,
    onToggleBookmark: (TranslationRecord) -> Unit,
    onSubmitFeedback: (TranslationRecord, String, IntentCode?) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onRecordNewMeow: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTimeFilter by remember { mutableStateOf("All time") }
    var selectedCategoryFilter by remember { mutableStateOf("All Meows") }

    val now = System.currentTimeMillis()
    val filtered = remember(translations, searchQuery, selectedTimeFilter, selectedCategoryFilter) {
        translations.filter { record ->
            val matchesQuery = searchQuery.isBlank() ||
                record.title.contains(searchQuery, ignoreCase = true) ||
                record.catName.contains(searchQuery, ignoreCase = true) ||
                record.location.contains(searchQuery, ignoreCase = true) ||
                record.intentTag.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedCategoryFilter) {
                "Hunger" -> record.intentCode == "F"
                "Affection" -> record.intentCode == "B"
                "Needs Care" -> record.intentCode == "I"
                "Bookmarked" -> record.isBookmarked
                else -> true
            }

            val ageHours = (now - record.createdAtMillis) / (1000L * 60L * 60L)
            val matchesTime = when (selectedTimeFilter) {
                "Today" -> record.timestampLabel.startsWith("Today", ignoreCase = true) || ageHours < 24
                "Last week" -> ageHours <= 24 * 7
                "This month" -> ageHours <= 24 * 31
                else -> true
            }
            matchesQuery && matchesCategory && matchesTime
        }
    }

    val totalCount = translations.size
    val topGroup = translations.groupBy { it.intentTag }.maxByOrNull { it.value.size }
    val topPct = if (totalCount > 0 && topGroup != null) {
        (topGroup.value.size * 100) / totalCount
    } else 0

    Box(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("history_screen_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WeeklyReportBanner(
                    totalAnalyzed = totalCount,
                    topIntentLabel = topGroup?.key ?: "Hunger",
                    topIntentPct = topPct,
                    modifier = Modifier.widthIn(max = 600.dp)
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search meows by cat, intent, or location...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = OnSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Clear search",
                                        tint = OnSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceContainerLowest,
                            unfocusedContainerColor = SurfaceContainerLowest,
                            focusedBorderColor = PrimaryContainer,
                            unfocusedBorderColor = OutlineVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_search_input")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All time", "Today", "Last week", "This month").forEach { tf ->
                            FilterPill(
                                label = tf,
                                selected = selectedTimeFilter == tf,
                                onClick = { selectedTimeFilter = tf },
                                tag = "time_filter_$tf"
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All Meows", "Hunger", "Affection", "Needs Care", "Bookmarked").forEach { cat ->
                            FilterPill(
                                label = cat,
                                selected = selectedCategoryFilter == cat,
                                onClick = { selectedCategoryFilter = cat },
                                tag = "cat_filter_$cat"
                            )
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Surface(
                        color = SurfaceContainerLow,
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = null,
                                tint = PrimaryContainer,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No matching meow logs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            Text(
                                text = "Try adjusting your search filters or record a new meow in Translate.",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { record ->
                    HistoryRecordCard(
                        record = record,
                        isPlaying = playingAudioId == "hist_play_${record.id}",
                        showPersian = showPersian,
                        onPlay = { onPlayAudio(record) },
                        onToggleBookmark = { onToggleBookmark(record) },
                        onFeedback = { verdict, corrected -> onSubmitFeedback(record, verdict, corrected) },
                        onDelete = { onDeleteRecord(record.id) },
                        modifier = Modifier.widthIn(max = 600.dp)
                    )
                }
            }
        }

        Button(
            onClick = onRecordNewMeow,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryContainer,
                contentColor = OnPrimary
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .minimumInteractiveComponentSize()
                .testTag("history_record_new_cta")
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Record New Meow",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun WeeklyReportBanner(
    totalAnalyzed: Int,
    topIntentLabel: String,
    topIntentPct: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = SecondaryFixed,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(PrimaryFixed, SecondaryFixed)
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Weekly Voice Report",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = OnPrimaryFixed
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = SurfaceContainerLowest.copy(alpha = 0.86f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "TOTAL ANALYZED",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant
                        )
                        Text(
                            text = "$totalAnalyzed meows",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnPrimaryFixed
                        )
                    }
                }
                Surface(
                    color = SurfaceContainerLowest.copy(alpha = 0.86f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "TOP VOCAL STATE",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant
                        )
                        Text(
                            text = "$topIntentLabel ($topIntentPct%)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        onClick = onClick,
        color = if (selected) PrimaryContainer else SurfaceContainerLow,
        shape = CircleShape,
        border = BorderStroke(
            1.dp,
            if (selected) PrimaryContainer else OutlineVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag(tag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) OnPrimary else OnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun HistoryRecordCard(
    record: TranslationRecord,
    isPlaying: Boolean,
    showPersian: Boolean,
    onPlay: () -> Unit,
    onToggleBookmark: () -> Unit,
    onFeedback: (String, IntentCode?) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCorrector by remember(record.id) { mutableStateOf(false) }

    val (accentColor, badgeBg, icon) = when (record.intentCode) {
        "F" -> Triple(FoodAmber, FoodAmberContainer, Icons.Filled.Restaurant)
        "I" -> Triple(DistressPeriwinkle, DistressPeriwinkleContainer, Icons.Filled.WarningAmber)
        else -> Triple(AffectionCoral, AffectionCoralContainer, Icons.Filled.Favorite)
    }

    val waveformBars = remember(record.waveformCsv) {
        record.waveformCsv.split(",").mapNotNull { it.toFloatOrNull() }.ifEmpty {
            listOf(0.3f, 0.5f, 0.8f, 0.9f, 0.7f, 0.4f, 0.6f, 0.8f, 0.5f, 0.3f)
        }
    }

    Surface(
        color = SurfaceContainerLowest,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(badgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = record.intentTag,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = record.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${record.catName} • ${record.timestampLabel} • ${record.location}",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("history_bookmark_${record.id}")
                    ) {
                        Icon(
                            imageVector = if (record.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Toggle bookmark",
                            tint = if (record.isBookmarked) Primary else OnSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("history_delete_${record.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete entry",
                            tint = OnSurfaceVariant
                        )
                    }
                }
            }

            if (showPersian) {
                Text(
                    text = record.titleFa,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = VazirmatnFamily,
                        fontSize = 12.sp
                    ),
                    color = OnSurfaceVariant
                )
            }

            // Playback + Waveform Strip
            Surface(
                color = SurfaceContainerLow,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = onPlay,
                        shape = CircleShape,
                        color = PrimaryContainer,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("history_play_${record.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause clip" else "Play clip",
                                tint = OnPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        waveformBars.forEach { v ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height((6f + v * 20f).coerceIn(6f, 26f).dp)
                                    .clip(CircleShape)
                                    .background(if (isPlaying) Primary else PrimaryContainer.copy(alpha = 0.55f))
                            )
                        }
                    }

                    Text(
                        text = "${record.pitchHz} • ${record.durationSec}",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = ConfidenceEmeraldBg,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${record.confidence}% Confidence • ${record.intentTag}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ConfidenceEmerald,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (record.feedbackStatus != null && !showCorrector) {
                        Surface(
                            color = SurfaceContainer,
                            shape = CircleShape,
                            modifier = Modifier.clickable { showCorrector = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = record.feedbackStatus,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                onClick = {
                                    showCorrector = false
                                    onFeedback("Correct", null)
                                },
                                color = SurfaceContainer,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "Accurate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ConfidenceEmerald,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                onClick = { showCorrector = !showCorrector },
                                color = SurfaceContainer,
                                shape = CircleShape
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "Edit Tag",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Primary
                                    )
                                }
                            }
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { (record.confidence / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    color = PrimaryContainer,
                    trackColor = SurfaceContainerHigh,
                    strokeCap = StrokeCap.Round
                )

                if (showCorrector) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            IntentCode.FOOD to "Hunger",
                            IntentCode.BRUSHING to "Affection",
                            IntentCode.ISOLATION to "Needs Care"
                        ).forEach { (code, tagLabel) ->
                            Surface(
                                onClick = {
                                    showCorrector = false
                                    onFeedback("Wrong", code)
                                },
                                color = PrimaryFixed,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "Set: $tagLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnPrimaryFixed,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
