package com.example.ui.modals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.billing.MyketBillingState
import com.example.billing.MyketIabHelper
import com.example.data.model.PetProfile
import com.example.ui.components.CatAvatarImage
import com.example.ui.theme.ConfidenceEmerald
import com.example.ui.theme.ConfidenceEmeraldBg
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
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwitchPetBottomSheet(
    pets: List<PetProfile>,
    onSelectPet: (String) -> Unit,
    onAddNewPet: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalMeowStrings.current
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.switchCatModalTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurface
                    )
                    Text(
                        text = strings.switchCatModalSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = strings.closeButton, tint = OnSurfaceVariant)
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
                                            text = strings.activeBadge,
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
                                contentDescription = strings.activeBadge,
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
                Text(strings.addNewCatButton)
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
    val strings = LocalMeowStrings.current
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
                    text = if (isNew) strings.addCatModalTitle else "${strings.editCatModalPrefix}${existingPet?.name}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = strings.closeButton)
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(strings.catNameLabel) },
                placeholder = { Text(strings.catNamePlaceholder) },
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
                Text(strings.breedLabel, style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StepperBox(
                    label = strings.ageYearsLabel,
                    value = ageYears,
                    onDec = { if (ageYears > 0) ageYears-- },
                    onInc = { if (ageYears < 25) ageYears++ },
                    modifier = Modifier.weight(1f)
                )
                StepperBox(
                    label = strings.ageMonthsLabel,
                    value = ageMonths,
                    onDec = { if (ageMonths > 0) ageMonths-- },
                    onInc = { if (ageMonths < 11) ageMonths++ },
                    modifier = Modifier.weight(1f)
                )
            }

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val displayedWeight = if (useLbs) "%.1f ${strings.useLbs}".format(weightKg * 2.20462) else "%.1f ${strings.useKg}".format(weightKg)
                Column {
                    Text(strings.weightLabel, style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
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
                            text = if (useLbs) strings.useKg else strings.useLbs,
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(strings.coatPatternLabel, style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
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
                            text = if (baselineSampleCaptured) strings.baselineCalibrated else strings.attachBaselineCalibration,
                            style = MaterialTheme.typography.titleSmall,
                            color = OnSurface
                        )
                        Text(
                            text = strings.baselineCalibrationHint,
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
                Text(if (isNew) strings.saveCalibrateCat else strings.saveChanges)
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
    billingState: MyketBillingState,
    onPurchaseSku: (String) -> Unit,
    onRestorePurchases: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalMeowStrings.current
    var selectedPlanSku by remember { mutableStateOf(MyketIabHelper.SKU_PRO_LIFETIME) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val monthlyPriceText = billingState.skuPrices[MyketIabHelper.SKU_PRO_MONTHLY]
        ?.takeIf { it.isNotBlank() } ?: strings.monthlyPlanPrice
    val lifetimePriceText = billingState.skuPrices[MyketIabHelper.SKU_PRO_LIFETIME]
        ?.takeIf { it.isNotBlank() } ?: strings.lifetimePlanPrice

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
                            text = strings.proModalTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnPrimaryFixed
                        )
                        Text(
                            text = strings.proModalSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnPrimaryContainer
                        )
                    }
                }
            }

            listOf(
                strings.proPerk1,
                strings.proPerk2,
                strings.proPerk3,
                strings.proPerk4
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
                    title = strings.monthlyPlanTitle,
                    price = monthlyPriceText,
                    badge = strings.monthlyPlanBadge,
                    selected = selectedPlanSku == MyketIabHelper.SKU_PRO_MONTHLY,
                    onClick = { selectedPlanSku = MyketIabHelper.SKU_PRO_MONTHLY },
                    tag = "pro_plan_monthly",
                    modifier = Modifier.weight(1f)
                )
                PlanCard(
                    title = strings.lifetimePlanTitle,
                    price = lifetimePriceText,
                    badge = strings.lifetimePlanBadge,
                    selected = selectedPlanSku == MyketIabHelper.SKU_PRO_LIFETIME,
                    onClick = { selectedPlanSku = MyketIabHelper.SKU_PRO_LIFETIME },
                    tag = "pro_plan_lifetime",
                    modifier = Modifier.weight(1f)
                )
            }

            // Myket Billing Connection / Key Status Pill
            Surface(
                color = if (billingState.isBillingSupported && billingState.isPublicKeyConfigured) {
                    ConfidenceEmeraldBg
                } else {
                    SurfaceContainerLow
                },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (billingState.isBillingSupported) Icons.Filled.Verified else Icons.Filled.Info,
                        contentDescription = null,
                        tint = if (billingState.isBillingSupported) ConfidenceEmerald else Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = when {
                            !billingState.isPublicKeyConfigured -> strings.myketMissingKeyHint
                            billingState.isBillingSupported -> strings.myketReadyBadge
                            else -> strings.myketNotReadyHint
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }

            Button(
                onClick = {
                    if (!isProMember) {
                        onPurchaseSku(selectedPlanSku)
                    }
                },
                enabled = !billingState.isBusy,
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
                if (billingState.isBusy) {
                    CircularProgressIndicator(
                        color = OnPrimary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        when {
                            isProMember -> strings.proAlreadyActiveButton
                            selectedPlanSku == MyketIabHelper.SKU_PRO_MONTHLY -> strings.buyWithMyketMonthly
                            else -> strings.buyWithMyketLifetime
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onRestorePurchases,
                    enabled = !billingState.isBusy,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, OutlineVariant),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("myket_restore_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.restoreMyketPurchases, color = Primary, maxLines = 1)
                }
                OutlinedButton(
                    onClick = onDismiss,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, OutlineVariant),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(strings.maybeLaterButton, color = OnSurfaceVariant)
                }
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
    tag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (selected) PrimaryFixed else SurfaceContainerLowest,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, if (selected) PrimaryContainer else OutlineVariant),
        modifier = modifier.testTag(tag)
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
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Primary
            )
        }
    }
}

@Composable
fun HowAiWorksDialog(onDismiss: () -> Unit) {
    val strings = LocalMeowStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.howAiWorksTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.howAiDialogBody1, style = MaterialTheme.typography.bodyMedium)
                Text(strings.howAiDialogBody2, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                Text(strings.howAiDialogBody3, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                Text(strings.howAiDialogBody4, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.gotItButton) }
        }
    )
}

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    val strings = LocalMeowStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.privacyPolicyTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.privacyDialogBody1, style = MaterialTheme.typography.bodyMedium)
                Text(strings.privacyDialogBody2, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.closeButton) }
        }
    )
}
