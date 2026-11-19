package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.AppSettings
import com.example.data.model.ModelEngineType
import com.example.data.model.PetProfile
import com.example.ui.components.CatAvatarImage
import com.example.ui.theme.Error
import com.example.ui.theme.LocalMeowStrings
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

@Composable
fun SettingsScreen(
    activePet: PetProfile?,
    settings: AppSettings,
    feedbackCount: Int,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectEngine: (ModelEngineType) -> Unit,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onSwitchPet: () -> Unit,
    onExportHistory: () -> Unit,
    onClearHistoryAndCache: () -> Unit,
    onOpenProModal: () -> Unit,
    onOpenHowAiWorks: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalMeowStrings.current
    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(strings.clearDialogTitle) },
            text = { Text(strings.clearDialogBody) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirm = false
                        onClearHistoryAndCache()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Error)
                ) {
                    Text(strings.clearAllButton)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(strings.cancelButton)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Active Pet Context Card
        item {
            Surface(
                color = SurfaceContainerLow,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        CatAvatarImage(pet = activePet, size = 46.dp)
                        Column {
                            Text(
                                text = "${strings.calibratedForPrefix}${activePet?.name ?: "Lucy"}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            Text(
                                text = "${activePet?.breed ?: "Ginger Tabby"} • ${activePet?.baselinePitchHz ?: 420}${strings.hzBaselineSuffix}",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onSwitchPet,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, OutlineVariant),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SwapHoriz,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.switchButton, color = Primary)
                    }
                }
            }
        }

        // Bilingual Language Selector Card
        item {
            Surface(
                color = SurfaceContainerLowest,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeader(
                        icon = Icons.Filled.Language,
                        title = strings.appLanguageTitle
                    )
                    Text(
                        text = strings.appLanguageSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            val picked = settings.language == lang
                            Surface(
                                onClick = { onSelectLanguage(lang) },
                                color = if (picked) PrimaryContainer else SurfaceContainerLow,
                                shape = CircleShape,
                                border = BorderStroke(
                                    1.dp,
                                    if (picked) PrimaryContainer else OutlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("settings_lang_${lang.code}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = lang.displayName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (picked) OnPrimary else OnSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // AI Translation & Audio Engine
        item {
            Surface(
                color = SurfaceContainerLowest,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionHeader(
                        icon = Icons.Filled.Psychology,
                        title = strings.aiEngineSectionTitle
                    )

                    Text(
                        text = strings.inferenceArchitectureLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModelEngineType.entries.forEach { engine ->
                            val selected = settings.modelEngine == engine
                            Surface(
                                onClick = { onSelectEngine(engine) },
                                color = if (selected) PrimaryFixed else SurfaceContainerLow,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) PrimaryContainer else OutlineVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_engine_${engine.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = strings.engineName(engine),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selected) OnPrimaryFixed else OnSurface
                                            )
                                            Surface(
                                                color = if (selected) PrimaryContainer else SurfaceContainerHigh,
                                                shape = CircleShape
                                            ) {
                                                Text(
                                                    text = strings.engineAccuracy(engine),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (selected) OnPrimary else OnSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = strings.engineSubtitle(engine),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (selected) OnPrimaryContainer else OnSurfaceVariant
                                        )
                                    }
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = Primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = OutlineVariant.copy(alpha = 0.35f))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = strings.sensitivityLevelLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Low" to strings.sensLow,
                                "Medium" to strings.sensMedium,
                                "High" to strings.sensHigh
                            ).forEach { (levelKey, levelLabel) ->
                                val picked = settings.sensitivityLevel == levelKey
                                Surface(
                                    onClick = { onUpdateSettings { it.copy(sensitivityLevel = levelKey) } },
                                    color = if (picked) PrimaryContainer else SurfaceContainerLow,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("sensitivity_$levelKey")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = levelLabel,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (picked) OnPrimary else OnSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = strings.audioSampleQualityLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Studio HD" to strings.qualityHd,
                                "Balanced 16kHz" to strings.qualityBalanced
                            ).forEach { (qKey, qLabel) ->
                                val picked = settings.audioQuality == qKey
                                Surface(
                                    onClick = { onUpdateSettings { it.copy(audioQuality = qKey) } },
                                    color = if (picked) PrimaryContainer else SurfaceContainerLow,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = qLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (picked) OnPrimary else OnSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    SettingsToggleRow(
                        title = strings.noiseFilterTitle,
                        subtitle = strings.noiseFilterSubtitle,
                        checked = settings.noiseFilterEnabled,
                        onCheckedChange = { v -> onUpdateSettings { it.copy(noiseFilterEnabled = v) } }
                    )
                    SettingsToggleRow(
                        title = strings.autoSaveTitle,
                        subtitle = strings.autoSaveSubtitle,
                        checked = settings.autoRecordMeows,
                        onCheckedChange = { v -> onUpdateSettings { it.copy(autoRecordMeows = v) } }
                    )
                    SettingsToggleRow(
                        title = strings.continuousDetectionTitle,
                        subtitle = strings.continuousDetectionSubtitle,
                        checked = settings.continuousDetection,
                        onCheckedChange = { v -> onUpdateSettings { it.copy(continuousDetection = v) } }
                    )
                    SettingsToggleRow(
                        title = strings.bilingualSubtitlesTitle,
                        subtitle = strings.bilingualSubtitlesSubtitle,
                        checked = settings.showPersianSubtitles,
                        onCheckedChange = { v -> onUpdateSettings { it.copy(showPersianSubtitles = v) } }
                    )
                }
            }
        }

        // Notifications & Alerts
        item {
            Surface(
                color = SurfaceContainerLowest,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeader(
                        icon = Icons.Filled.NotificationsActive,
                        title = strings.notificationsTitle
                    )
                    SettingsToggleRow(
                        title = strings.instantAlertsTitle,
                        subtitle = strings.instantAlertsSubtitle,
                        checked = settings.instantMeowAlerts,
                        onCheckedChange = { v -> onUpdateSettings { it.copy(instantMeowAlerts = v) } }
                    )
                    SettingsToggleRow(
                        title = strings.dailySummaryTitle,
                        subtitle = strings.dailySummarySubtitle,
                        checked = settings.dailyBehaviorSummary,
                        onCheckedChange = { v -> onUpdateSettings { it.copy(dailyBehaviorSummary = v) } }
                    )
                    SettingsToggleRow(
                        title = strings.feedingSyncTitle,
                        subtitle = strings.feedingSyncSubtitle,
                        checked = settings.feedingReminderSync,
                        onCheckedChange = { v -> onUpdateSettings { it.copy(feedingReminderSync = v) } }
                    )
                }
            }
        }

        // Data & Storage
        item {
            Surface(
                color = SurfaceContainerLowest,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeader(
                        icon = Icons.Filled.GraphicEq,
                        title = strings.dataStorageTitle
                    )
                    SettingsToggleRow(
                        title = strings.saveAudioLocallyTitle,
                        subtitle = strings.saveAudioLocallySubtitle,
                        checked = settings.saveAudioLocally,
                        onCheckedChange = { v -> onUpdateSettings { it.copy(saveAudioLocally = v) } }
                    )

                    HorizontalDivider(color = OutlineVariant.copy(alpha = 0.35f))

                    SettingsActionRow(
                        icon = Icons.Filled.CloudDownload,
                        title = strings.exportCsvTitle,
                        subtitle = strings.exportCsvSubtitle,
                        onClick = onExportHistory,
                        tag = "export_history_button"
                    )
                    SettingsActionRow(
                        icon = Icons.Filled.DeleteSweep,
                        title = strings.clearCacheTitle,
                        subtitle = strings.clearCacheSubtitle,
                        isDestructive = true,
                        onClick = { showClearConfirm = true },
                        tag = "clear_history_button"
                    )
                }
            }
        }

        // Meow Pro Card
        item {
            Surface(
                onClick = onOpenProModal,
                shape = RoundedCornerShape(24.dp),
                color = SecondaryFixed,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .testTag("settings_meow_pro_card")
            ) {
                Row(
                    modifier = Modifier
                        .background(Brush.linearGradient(listOf(PrimaryFixed, SecondaryFixed)))
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(PrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WorkspacePremium,
                                contentDescription = null,
                                tint = OnPrimary
                            )
                        }
                        Column {
                            Text(
                                text = if (settings.isProMember) strings.proActiveTitle else strings.upgradeProTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = OnPrimaryFixed
                            )
                            Text(
                                text = strings.proCardSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = OnPrimaryContainer
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = OnPrimaryFixed
                    )
                }
            }
        }

        // Support & About
        item {
            Surface(
                color = SurfaceContainerLowest,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeader(
                        icon = Icons.Filled.Info,
                        title = strings.supportAboutTitle
                    )
                    SettingsActionRow(
                        icon = Icons.Filled.Psychology,
                        title = strings.howAiWorksTitle,
                        subtitle = strings.howAiWorksSubtitle,
                        onClick = onOpenHowAiWorks,
                        tag = "how_ai_works_button"
                    )
                    SettingsActionRow(
                        icon = Icons.Filled.PrivacyTip,
                        title = strings.privacyPolicyTitle,
                        subtitle = strings.privacyPolicySubtitleFormat.format(feedbackCount),
                        onClick = onOpenPrivacyPolicy,
                        tag = "privacy_policy_button"
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.versionFooter,
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OnSurface
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = OnSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OnPrimary,
                checkedTrackColor = PrimaryContainer,
                uncheckedTrackColor = SurfaceContainerHigh
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(vertical = 6.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDestructive) Error.copy(alpha = 0.12f) else SurfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) Error else Primary,
                modifier = Modifier.size(19.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDestructive) Error else OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = OnSurfaceVariant
        )
    }
}
