package com.example.ui.modals

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PetProfile
import com.example.ui.components.CatAvatarImage
import com.example.ui.theme.ConfidenceEmerald
import com.example.ui.theme.ConfidenceEmeraldBg
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
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwitchPetBottomSheet(
    pets: List<PetProfile>,
    onSelectPet: (String) -> Unit,
    onAddNewPet: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Switch Cat Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurface
                    )
                    Text(
                        text = "Tailor pitch calibration & history to your active cat",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = OnSurfaceVariant)
                }
            }

            pets.forEach { pet ->
                Surface(
                    onClick = {
                        onSelectPet(pet.id)
                        onDismiss()
                    },
                    color = if (pet.isActive) PrimaryFixed.copy(alpha = 0.6f) else SurfaceContainerLowest,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        1.5.dp,
                        if (pet.isActive) PrimaryContainer else OutlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("switch_pet_item_${pet.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CatAvatarImage(pet = pet, size = 48.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = pet.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                                if (pet.isActive) {
                                    Surface(color = PrimaryContainer, shape = CircleShape) {
                                        Text(
                                            text = "Active",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = OnPrimary,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${pet.breed} • ${pet.ageDisplay} • ${pet.baselinePitchHz} Hz",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                        if (pet.isActive) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Selected pet",
                                tint = Primary
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onDismiss()
                    onAddNewPet()
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryContainer,
                    contentColor = OnPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("switch_sheet_add_pet_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add New Cat Profile")
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditPetBottomSheet(
    existingPet: PetProfile?,
    onSavePet: (PetProfile, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val isNew = existingPet == null
    var name by remember(existingPet) { mutableStateOf(existingPet?.name ?: "") }
    var breed by remember(existingPet) { mutableStateOf(existingPet?.breed ?: "Domestic Shorthair") }
    var ageYears by remember(existingPet) { mutableIntStateOf(existingPet?.ageYears ?: 1) }
    var ageMonths by remember(existingPet) { mutableIntStateOf(existingPet?.ageMonths ?: 6) }
    var sex by remember(existingPet) { mutableStateOf(existingPet?.sex ?: "Female") }
    var repro by remember(existingPet) { mutableStateOf(existingPet?.reproductiveStatus ?: "Spayed / Neutered") }
    var weightKg by remember(existingPet) { mutableDoubleStateOf(existingPet?.weightKg ?: 4.0) }
    var useLbs by remember { mutableStateOf(false) }
    var coatTag by remember(existingPet) { mutableStateOf(existingPet?.coatTags ?: "Ginger / Orange") }
    var baselineSampleCaptured by remember { mutableStateOf(!isNew) }

    val breeds = listOf(
        "Domestic Shorthair",
        "Ginger Tabby",
        "British Shorthair",
        "Siamese Mix",
        "Maine Coon",
        "Persian"
    )
    val coatOptions = listOf("Ginger / Orange", "Tuxedo / Bicolor", "Gray / Blue", "Tabby / Striped", "Calico")

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isNew) "Add Cat Profile" else "Edit ${existingPet?.name}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Cat Name") },
                placeholder = { Text("e.g., Cleo, Ginger, Luna") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceContainerLowest,
                    unfocusedContainerColor = SurfaceContainerLowest,
                    focusedBorderColor = PrimaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pet_name_input")
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Breed", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    breeds.forEach { b ->
                        val picked = breed == b
                        Surface(
                            onClick = { breed = b },
                            color = if (picked) PrimaryFixed else SurfaceContainerLow,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, if (picked) PrimaryContainer else OutlineVariant)
                        ) {
                            Text(
                                text = b,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (picked) OnPrimaryFixed else OnSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // Age Years / Months Steppers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StepperBox(
                    label = "Age (Years)",
                    value = ageYears,
                    onDec = { if (ageYears > 0) ageYears-- },
                    onInc = { if (ageYears < 25) ageYears++ },
                    modifier = Modifier.weight(1f)
                )
                StepperBox(
                    label = "Age (Months)",
                    value = ageMonths,
                    onDec = { if (ageMonths > 0) ageMonths-- },
                    onInc = { if (ageMonths < 11) ageMonths++ },
                    modifier = Modifier.weight(1f)
                )
            }

            // Sex & Reproductive status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Female", "Male").forEach { s ->
                    val picked = sex == s
                    Surface(
                        onClick = { sex = s },
                        color = if (picked) PrimaryContainer else SurfaceContainerLow,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = s,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (picked) OnPrimary else OnSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Spayed / Neutered", "Intact").forEach { r ->
                    val picked = repro == r
                    Surface(
                        onClick = { repro = r },
                        color = if (picked) PrimaryFixed else SurfaceContainerLow,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, if (picked) PrimaryContainer else OutlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = r,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (picked) OnPrimaryFixed else OnSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Weight Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val displayedWeight = if (useLbs) "%.1f lbs".format(weightKg * 2.20462) else "%.1f kg".format(weightKg)
                Column {
                    Text("Weight", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                    Text(
                        text = displayedWeight,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { weightKg = (weightKg - 0.2).coerceAtLeast(0.5) }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease weight")
                    }
                    IconButton(onClick = { weightKg = (weightKg + 0.2).coerceAtMost(15.0) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase weight")
                    }
                    Surface(
                        onClick = { useLbs = !useLbs },
                        color = SurfaceContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = if (useLbs) "Use kg" else "Use lbs",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Coat & Pattern", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    coatOptions.forEach { tag ->
                        val picked = coatTag == tag
                        Surface(
                            onClick = { coatTag = tag },
                            color = if (picked) PrimaryFixed else SurfaceContainerLow,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, if (picked) PrimaryContainer else OutlineVariant)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (picked) OnPrimaryFixed else OnSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            Surface(
                onClick = { baselineSampleCaptured = !baselineSampleCaptured },
                color = if (baselineSampleCaptured) ConfidenceEmeraldBg else SurfaceContainerLow,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (baselineSampleCaptured) ConfidenceEmerald else OutlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (baselineSampleCaptured) Icons.Filled.CheckCircle else Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = if (baselineSampleCaptured) ConfidenceEmerald else Primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (baselineSampleCaptured) "Acoustic Baseline Calibrated" else "Attach Baseline Voice Calibration",
                            style = MaterialTheme.typography.titleSmall,
                            color = OnSurface
                        )
                        Text(
                            text = "Tunes F0 formant range (380–480 Hz) for breed accuracy",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val trimmedName = name.trim().ifEmpty { "Luna" }
                    val avatarKey = when {
                        existingPet != null -> existingPet.avatarKey
                        coatTag.contains("Tuxedo") || coatTag.contains("Gray") -> "oliver"
                        sex == "Male" -> "milo"
                        else -> "lucy"
                    }
                    val petToSave = (existingPet ?: PetProfile(
                        id = "cat-${UUID.randomUUID().toString().take(6)}",
                        name = trimmedName,
                        breed = breed,
                        ageYears = ageYears,
                        ageMonths = ageMonths,
                        avatarKey = avatarKey
                    )).copy(
                        name = trimmedName,
                        breed = breed,
                        ageYears = ageYears,
                        ageMonths = ageMonths,
                        sex = sex,
                        reproductiveStatus = repro,
                        weightKg = "%.1f".format(weightKg).toDoubleOrNull() ?: weightKg,
                        coatTags = coatTag,
                        calibratedPct = if (baselineSampleCaptured) 100 else 65
                    )
                    onSavePet(petToSave, isNew)
                    onDismiss()
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryContainer,
                    contentColor = OnPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("save_pet_profile_button")
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isNew) "Save & Calibrate Cat" else "Save Changes")
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun StepperBox(
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
                IconButton(onClick = onDec, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease $label")
                }
                Text(
                    text = "$value",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = onInc, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase $label")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeowProBottomSheet(
    isProMember: Boolean,
    onActivateProPlaceholder: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPlan by remember { mutableStateOf("Lifetime") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = SecondaryFixed,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .background(Brush.linearGradient(listOf(PrimaryFixed, SecondaryFixed)))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
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
                            text = "Meow Pro Bioacoustics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnPrimaryFixed
                        )
                        Text(
                            text = "Deepest feline vocal insight & multi-pet studio tools",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnPrimaryContainer
                        )
                    }
                }
            }

            listOf(
                "Unlimited multi-cat profiles & custom vocal calibration",
                "Continuous background meow monitoring mode",
                "Full bioacoustic pitch (F0) & spectrogram export",
                "Priority active-learning model personalization"
            ).forEach { perk ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = PrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = perk,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlanCard(
                    title = "Monthly",
                    price = "$4.99/mo",
                    badge = "Flexible",
                    selected = selectedPlan == "Monthly",
                    onClick = { selectedPlan = "Monthly" },
                    modifier = Modifier.weight(1f)
                )
                PlanCard(
                    title = "Lifetime",
                    price = "$29.99",
                    badge = "Best Value",
                    selected = selectedPlan == "Lifetime",
                    onClick = { selectedPlan = "Lifetime" },
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = {
                    // TODO: Connect Google Play Billing Library for real in-app purchase flow
                    onActivateProPlaceholder()
                    onDismiss()
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryContainer,
                    contentColor = OnPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("meow_pro_upgrade_button")
            ) {
                Text(
                    if (isProMember) "Pro Mode Enabled (Preview)"
                    else "Start $selectedPlan Plan (Billing Preview)"
                )
            }

            OutlinedButton(
                onClick = onDismiss,
                shape = CircleShape,
                border = BorderStroke(1.dp, OutlineVariant),
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
            ) {
                Text("Maybe Later", color = OnSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    badge: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (selected) PrimaryFixed else SurfaceContainerLowest,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, if (selected) PrimaryContainer else OutlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                color = if (selected) PrimaryContainer else SurfaceContainer,
                shape = CircleShape
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) OnPrimary else OnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )
            Text(
                text = price,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Primary
            )
        }
    }
}

@Composable
fun HowAiWorksDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How Meow AI Works") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Meow runs a 100% on-device dual-branch bioacoustic engine trained on real feline vocalization datasets:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "1. Denoising DSP: Removes DC offset, applies a 70 Hz 2nd-order Butterworth high-pass filter, and trims silence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
                Text(
                    "2. Enhanced Bioacoustic Pipeline (74.6% Acc): Extracts 234 features including YIN pitch (F0), 20 MFCCs + deltas, chroma, spectral contrast, and temporal envelope.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
                Text(
                    "3. Spectrogram CNN (76.2% Acc) & Hybrid Late Fusion (79.4% Acc): Blends 45% Bioacoustic + 55% 128-bin Mel Spectrogram CNN probabilities.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Got it") }
        }
    )
}

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Privacy & Vocal Data Policy") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "All audio recording, signal denoising, and AI intent classification run locally on your device.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "• Microphone access is only used when you press Record.\n• Pet profiles, translation history, and active-learning feedback are stored locally in your device Room database.\n• Exported CSV logs or shared translations are only sent when you explicitly tap Share or Export.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
