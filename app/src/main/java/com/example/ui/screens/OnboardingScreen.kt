package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.PetProfile
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
import com.example.ui.theme.Surface
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest
import java.util.Locale
import java.util.UUID

@Composable
fun OnboardingScreen(
    customAvatarUri: Uri?,
    onPickPhoto: () -> Unit,
    onToggleLanguage: () -> Unit,
    onCompleteOnboarding: (PetProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalMeowStrings.current
    val isFa = strings.language == AppLanguage.FA

    var name by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var selectedAvatarKey by remember { mutableStateOf("lucy") }
    var breed by remember { mutableStateOf("Domestic Shorthair") }
    var ageYears by remember { mutableIntStateOf(1) }
    var ageMonths by remember { mutableIntStateOf(6) }
    var sex by remember { mutableStateOf("Female") }
    var repro by remember { mutableStateOf("Spayed / Neutered") }
    var weightKg by remember { mutableDoubleStateOf(4.0) }
    var coatTag by remember { mutableStateOf("Ginger / Orange") }
    var mood by remember { mutableStateOf("Playful & Demanding") }

    val breedOptions = remember(isFa) {
        if (isFa) {
            listOf(
                "Domestic Shorthair" to "موکوتاه خانگی",
                "Persian" to "پرشین",
                "Ginger Tabby" to "نارنجی راه‌راه",
                "British Shorthair" to "بریتیش",
                "Scottish Fold" to "اسکاتیش فولد",
                "Siamese Mix" to "سیامی",
                "Maine Coon" to "مین‌کون"
            )
        } else {
            listOf(
                "Domestic Shorthair" to "Domestic Shorthair",
                "Persian" to "Persian",
                "Ginger Tabby" to "Ginger Tabby",
                "British Shorthair" to "British Shorthair",
                "Scottish Fold" to "Scottish Fold",
                "Siamese Mix" to "Siamese Mix",
                "Maine Coon" to "Maine Coon"
            )
        }
    }

    val coatOptions = remember(isFa) {
        if (isFa) {
            listOf(
                "Ginger / Orange" to "نارنجی / حنایی",
                "White" to "سفید",
                "Black" to "مشکی",
                "Gray / Blue" to "طوسی / آبی",
                "Tuxedo / Bicolor" to "دو‌رنگ (تاکسیدو)",
                "Calico" to "سه‌رنگ (کالیکو)"
            )
        } else {
            listOf(
                "Ginger / Orange" to "Ginger / Orange",
                "White" to "White",
                "Black" to "Black",
                "Gray / Blue" to "Gray / Blue",
                "Tuxedo / Bicolor" to "Tuxedo / Bicolor",
                "Calico" to "Calico"
            )
        }
    }

    val moodOptions = remember(isFa) {
        if (isFa) {
            listOf(
                "Playful & Demanding" to "بازیگوش و پرانرژی",
                "Calm & Affectionate" to "آرام و بغلی",
                "Food Motivated" to "شکمو و غذا‌دوست",
                "Chatty & Social" to "پرحرف و اجتماعی",
                "Shy & Gentle" to "خجالتی و ملایم"
            )
        } else {
            listOf(
                "Playful & Demanding" to "Playful & Demanding",
                "Calm & Affectionate" to "Calm & Affectionate",
                "Food Motivated" to "Food Motivated",
                "Chatty & Social" to "Chatty & Social",
                "Shy & Gentle" to "Shy & Gentle"
            )
        }
    }

    val estimatedPitchHz = remember(ageYears, sex) {
        when {
            ageYears == 0 -> 510
            sex == "Male" -> 385
            else -> 430
        }
    }

    Surface(
        color = Surface,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Top bar with logo & language switcher
            Row(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .shadow(4.dp, CircleShape, ambientColor = PrimaryContainer, spotColor = PrimaryContainer)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_meow_logo),
                            contentDescription = strings.appName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                        )
                    }
                    Text(
                        text = strings.appName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurface
                    )
                }

                Surface(
                    onClick = onToggleLanguage,
                    color = SurfaceContainer,
                    shape = CircleShape,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("onboarding_language_toggle")
                ) {
                    Text(
                        text = strings.languageShortToggle,
                        style = MaterialTheme.typography.labelLarge,
                        color = Primary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            // Scrollable form
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 600.dp)
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero welcome banner
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = SecondaryFixed,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .background(Brush.linearGradient(listOf(PrimaryFixed, SecondaryFixed)))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = PrimaryContainer,
                            shape = CircleShape
                        ) {
                            Text(
                                text = strings.onboardingBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = OnPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = strings.onboardingTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnPrimaryFixed
                        )
                        Text(
                            text = strings.onboardingSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnPrimaryContainer
                        )
                    }
                }

                // Avatar & Photo Picker Card
                Surface(
                    color = SurfaceContainerLowest,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .border(3.dp, PrimaryContainer, CircleShape)
                                    .padding(4.dp)
                            ) {
                                if (customAvatarUri != null) {
                                    AsyncImage(
                                        model = customAvatarUri,
                                        contentDescription = strings.onboardingChoosePhoto,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    val resId = when (selectedAvatarKey) {
                                        "milo" -> R.drawable.img_cat_milo
                                        "oliver" -> R.drawable.img_cat_oliver
                                        else -> R.drawable.img_cat_lucy
                                    }
                                    Image(
                                        painter = painterResource(id = resId),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                }
                            }
                            Surface(
                                onClick = onPickPhoto,
                                color = PrimaryContainer,
                                shape = CircleShape,
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("onboarding_pick_photo_fab")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.CameraAlt,
                                        contentDescription = strings.onboardingChoosePhoto,
                                        tint = OnPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = onPickPhoto,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, OutlineVariant),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("onboarding_choose_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.onboardingChoosePhoto, color = Primary)
                        }

                        Text(
                            text = strings.onboardingPresetAvatar,
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                "lucy" to R.drawable.img_cat_lucy,
                                "milo" to R.drawable.img_cat_milo,
                                "oliver" to R.drawable.img_cat_oliver
                            ).forEach { (key, resId) ->
                                val isPicked = customAvatarUri == null && selectedAvatarKey == key
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isPicked) 2.5.dp else 1.dp,
                                            color = if (isPicked) PrimaryContainer else OutlineVariant,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedAvatarKey = key }
                                        .padding(3.dp)
                                        .testTag("onboarding_preset_$key")
                                ) {
                                    Image(
                                        painter = painterResource(id = resId),
                                        contentDescription = key,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }

                // Cat Details Form Card
                Surface(
                    color = SurfaceContainerLowest,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                if (it.isNotBlank()) nameError = false
                            },
                            label = { Text(strings.catNameLabel) },
                            placeholder = { Text(strings.catNamePlaceholder) },
                            isError = nameError,
                            supportingText = if (nameError) {
                                { Text(strings.onboardingValidationName) }
                            } else null,
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceContainerLowest,
                                unfocusedContainerColor = SurfaceContainerLowest,
                                focusedBorderColor = PrimaryContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_name_input")
                        )

                        // Breed chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(strings.breedLabel, style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                breedOptions.forEach { (key, label) ->
                                    val picked = breed == key
                                    Surface(
                                        onClick = { breed = key },
                                        color = if (picked) PrimaryFixed else SurfaceContainerLow,
                                        shape = CircleShape,
                                        border = BorderStroke(1.dp, if (picked) PrimaryContainer else OutlineVariant),
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
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

                        // Age Years & Months
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OnboardingStepperBox(
                                label = strings.ageYearsLabel,
                                value = ageYears,
                                onDec = { if (ageYears > 0) ageYears-- },
                                onInc = { if (ageYears < 25) ageYears++ },
                                modifier = Modifier.weight(1f)
                            )
                            OnboardingStepperBox(
                                label = strings.ageMonthsLabel,
                                value = ageMonths,
                                onDec = { if (ageMonths > 0) ageMonths-- },
                                onInc = { if (ageMonths < 11) ageMonths++ },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Sex toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Female" to strings.sexFemale, "Male" to strings.sexMale).forEach { (sKey, sLabel) ->
                                val picked = sex == sKey
                                Surface(
                                    onClick = { sex = sKey },
                                    color = if (picked) PrimaryContainer else SurfaceContainerLow,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("onboarding_sex_$sKey")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = sLabel,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (picked) OnPrimary else OnSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Reproductive status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Spayed / Neutered" to strings.reproNeutered,
                                "Intact" to strings.reproIntact
                            ).forEach { (rKey, rLabel) ->
                                val picked = repro == rKey
                                Surface(
                                    onClick = { repro = rKey },
                                    color = if (picked) PrimaryFixed else SurfaceContainerLow,
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, if (picked) PrimaryContainer else OutlineVariant),
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = rLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (picked) OnPrimaryFixed else OnSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Weight stepper
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceContainerLow)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(strings.weightLabel, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                                Text(
                                    text = String.format(Locale.US, "%.1f %s", weightKg, strings.useKg),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { weightKg = (weightKg - 0.2).coerceAtLeast(0.4) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease weight")
                                }
                                IconButton(
                                    onClick = { weightKg = (weightKg + 0.2).coerceAtMost(15.0) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase weight")
                                }
                            }
                        }

                        // Coat chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(strings.coatPatternLabel, style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                coatOptions.forEach { (key, label) ->
                                    val picked = coatTag == key
                                    Surface(
                                        onClick = { coatTag = key },
                                        color = if (picked) PrimaryFixed else SurfaceContainerLow,
                                        shape = CircleShape,
                                        border = BorderStroke(1.dp, if (picked) PrimaryContainer else OutlineVariant),
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
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

                        // Vocal personality / mood chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(strings.onboardingMoodLabel, style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                moodOptions.forEach { (key, label) ->
                                    val picked = mood == key
                                    Surface(
                                        onClick = { mood = key },
                                        color = if (picked) PrimaryFixed else SurfaceContainerLow,
                                        shape = CircleShape,
                                        border = BorderStroke(1.dp, if (picked) PrimaryContainer else OutlineVariant),
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
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
                    }
                }

                // Acoustic Baseline Calibration Preview
                Surface(
                    color = SurfaceContainerLow,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryFixed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = Primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.onboardingCalibrationPreview,
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant
                            )
                            Text(
                                text = "$estimatedPitchHz Hz • ${strings.baselineCalibrated}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }

            // Bottom Pinned Save CTA
            Surface(
                color = Surface.copy(alpha = 0.97f),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = {
                            val trimmed = name.trim()
                            if (trimmed.isEmpty()) {
                                nameError = true
                                return@Button
                            }
                            val pet = PetProfile(
                                id = "cat-${UUID.randomUUID().toString().take(6)}",
                                name = trimmed,
                                breed = breedOptions.firstOrNull { it.first == breed }?.second ?: breed,
                                ageYears = ageYears,
                                ageMonths = ageMonths,
                                sex = if (isFa) (if (sex == "Male") strings.sexMale else strings.sexFemale) else sex,
                                reproductiveStatus = if (isFa) {
                                    if (repro == "Intact") strings.reproIntact else strings.reproNeutered
                                } else repro,
                                weightKg = String.format(Locale.US, "%.1f", weightKg).toDoubleOrNull() ?: weightKg,
                                coatTags = coatOptions.firstOrNull { it.first == coatTag }?.second ?: coatTag,
                                calibratedPct = 100,
                                samplesAnalyzed = 0,
                                baselinePitchHz = estimatedPitchHz,
                                vocalCadence = if (ageYears == 0) "Chirpy Trill" else "Melodic",
                                dominantMood = moodOptions.firstOrNull { it.first == mood }?.second ?: mood,
                                avatarKey = selectedAvatarKey,
                                customAvatarUri = customAvatarUri?.toString(),
                                isActive = true
                            )
                            onCompleteOnboarding(pet)
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryContainer,
                            contentColor = OnPrimary
                        ),
                        contentPadding = PaddingValues(vertical = 15.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("onboarding_save_start_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.onboardingSaveAndStart,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepperBox(
    label: String,
    value: Int,
    onDec: () -> Unit,
    onInc: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDec,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease $label")
                }
                Text(
                    text = "$value",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
                IconButton(
                    onClick = onInc,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase $label")
                }
            }
        }
    }
}
