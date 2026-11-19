package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.PetProfile
import com.example.data.model.TranslationRecord
import com.example.ui.components.CatAvatarImage
import com.example.ui.theme.AffectionCoral
import com.example.ui.theme.DistressPeriwinkle
import com.example.ui.theme.FoodAmber
import com.example.ui.theme.LocalMeowStrings
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.OnPrimaryFixed
import com.example.ui.theme.OnSurface
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.OutlineVariant
import com.example.ui.theme.Primary
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.PrimaryFixed
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest

@Composable
fun ProfileScreen(
    activePet: PetProfile?,
    translations: List<TranslationRecord>,
    onPickPhoto: () -> Unit,
    onEditProfile: () -> Unit,
    onSwitchPet: () -> Unit,
    onAddPet: () -> Unit,
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalMeowStrings.current
    val isFa = strings.language == AppLanguage.FA
    val context = LocalContext.current
    val pet = activePet ?: return

    val petRecords = remember(translations, pet.name) {
        translations.filter { it.catName.equals(pet.name, ignoreCase = true) }
            .ifEmpty { translations }
    }
    val total = petRecords.size.coerceAtLeast(1)
    val foodPct = (petRecords.count { it.intentCode == "F" } * 100) / total
    val brushPct = (petRecords.count { it.intentCode == "B" } * 100) / total
    val carePct = (100 - foodPct - brushPct).coerceAtLeast(0)

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("profile_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Surface(
                color = SurfaceContainerLowest,
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .border(3.dp, PrimaryContainer, CircleShape)
                                .padding(4.dp)
                        ) {
                            CatAvatarImage(pet = pet, size = 96.dp)
                        }
                        Surface(
                            onClick = onPickPhoto,
                            color = PrimaryContainer,
                            shape = CircleShape,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("profile_edit_photo_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = "Photo",
                                    tint = OnPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = pet.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = OnSurface
                            )
                            Surface(
                                color = PrimaryFixed,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = strings.primaryCatBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnPrimaryFixed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = "${pet.breed} • ${pet.ageDisplay} • ${pet.sex}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                        Text(
                            text = "${pet.reproductiveStatus} • ${pet.coatTags}",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onEditProfile,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryContainer,
                                contentColor = OnPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("profile_edit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.editProfile)
                        }
                        OutlinedButton(
                            onClick = onSwitchPet,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, OutlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("profile_switch_pet_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SwapHoriz,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.switchPet, color = Primary)
                        }
                    }
                }
            }
        }

        item {
            Surface(
                color = SurfaceContainerLow,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = Primary
                            )
                            Text(
                                text = strings.voiceCalibrationTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                        }
                        Surface(
                            color = PrimaryFixed,
                            shape = CircleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "${pet.calibratedPct}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnPrimaryFixed
                                )
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (pet.calibratedPct / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = PrimaryContainer,
                        trackColor = SurfaceContainerHigh,
                        strokeCap = StrokeCap.Round
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AcousticBentoTile(
                            label = strings.samplesAnalyzedLabel,
                            value = "${pet.samplesAnalyzed} ${strings.clipsUnit}",
                            modifier = Modifier.weight(1f)
                        )
                        AcousticBentoTile(
                            label = strings.baselinePitchLabel,
                            value = "${pet.baselinePitchHz} Hz",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AcousticBentoTile(
                            label = strings.vocalCadenceLabel,
                            value = pet.vocalCadence,
                            modifier = Modifier.weight(1f)
                        )
                        AcousticBentoTile(
                            label = strings.dominantMoodLabel,
                            value = pet.dominantMood,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

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
                    Text(
                        text = strings.weeklyVocalTrends,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(CircleShape),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (foodPct > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(foodPct.toFloat())
                                    .height(12.dp)
                                    .background(FoodAmber)
                            )
                        }
                        if (brushPct > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(brushPct.toFloat())
                                    .height(12.dp)
                                    .background(AffectionCoral)
                            )
                        }
                        if (carePct > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(carePct.toFloat())
                                    .height(12.dp)
                                    .background(DistressPeriwinkle)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TrendLegendRow(strings.trendFood, "$foodPct%", FoodAmber)
                        TrendLegendRow(strings.trendAffection, "$brushPct%", AffectionCoral)
                        TrendLegendRow(strings.trendCare, "$carePct%", DistressPeriwinkle)
                    }
                }
            }
        }

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
                    Text(
                        text = strings.petBioHealth,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )

                    BioInfoRow(
                        icon = Icons.Filled.MonitorWeight,
                        label = strings.currentWeightLabel,
                        value = "${pet.weightKg} ${strings.useKg}"
                    )
                    HorizontalDivider(color = OutlineVariant.copy(alpha = 0.3f))
                    BioInfoRow(
                        icon = Icons.Filled.Cake,
                        label = strings.birthdayLabel,
                        value = pet.birthday
                    )
                    HorizontalDivider(color = OutlineVariant.copy(alpha = 0.3f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BioInfoRow(
                            icon = Icons.Filled.QrCode2,
                            label = strings.microchipIdLabel,
                            value = pet.microchipId,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Microchip ID", pet.microchipId))
                                onShowToast(
                                    if (isFa) "شناسه میکروچیپ کپی شد: ${pet.microchipId}"
                                    else "Copied microchip ID ${pet.microchipId}"
                                )
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("copy_microchip_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = strings.microchipIdLabel,
                                tint = Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = OutlineVariant.copy(alpha = 0.3f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BioInfoRow(
                            icon = Icons.Filled.LocalHospital,
                            label = strings.primaryVetLabel,
                            value = "${pet.vetName} • ${pet.vetClinic}",
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${pet.vetPhone}"))
                                context.startActivity(dialIntent)
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("call_vet_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Phone,
                                contentDescription = strings.primaryVetLabel,
                                tint = Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onAddPet,
                shape = CircleShape,
                border = BorderStroke(1.dp, PrimaryContainer),
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("profile_add_another_pet_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = Primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.addAnotherPet,
                    style = MaterialTheme.typography.labelLarge,
                    color = Primary
                )
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun AcousticBentoTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceContainerLowest,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
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
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TrendLegendRow(
    label: String,
    percentText: String,
    dotColor: Color
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
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurface
            )
        }
        Text(
            text = percentText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceVariant
        )
    }
}

@Composable
private fun BioInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
