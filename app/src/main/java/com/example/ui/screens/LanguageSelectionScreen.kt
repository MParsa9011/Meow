package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppLanguage
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.OnPrimaryContainer
import com.example.ui.theme.OnPrimaryFixed
import com.example.ui.theme.OnSurface
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.OutlineVariant
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.Primary
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.PrimaryFixed
import com.example.ui.theme.SelectedCardSurface
import com.example.ui.theme.Surface
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.VazirmatnFamily

@Composable
fun LanguageSelectionScreen(
    selectedLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onConfirmLanguage: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val isFa = selectedLanguage == AppLanguage.FA

    Surface(
        color = Surface,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Surface)
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 600.dp)
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Brand Logo Medallion (clean warm border on unified Surface)
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.5.dp, OutlineVariant.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_meow_logo),
                        contentDescription = "Meow",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Bilingual Hero Pill
                Surface(
                    color = PrimaryFixed,
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Language,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isFa) "انتخاب زبان پیش‌فرض" else "Default App Language",
                            fontFamily = if (isFa) VazirmatnFamily else PlusJakartaSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = OnPrimaryFixed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bilingual Title (Persian + English)
                Text(
                    text = "به میو خوش آمدید",
                    fontFamily = VazirmatnFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    lineHeight = 40.sp,
                    color = OnSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Welcome to Meow",
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isFa) {
                        "زبان دلخواه خود را برای ورود به برنامه انتخاب کنید. هر زمان بخواهید می‌توانید از تنظیمات یا نوار بالا زبان را تغییر دهید."
                    } else {
                        "Choose your preferred language to get started. You can switch languages anytime from Settings or the top bar."
                    },
                    fontFamily = if (isFa) VazirmatnFamily else PlusJakartaSansFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    lineHeight = if (isFa) 24.sp else 20.sp,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Language Option Cards (100% opaque, harmonized with warm Surface)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LanguageChoiceCard(
                        title = "فارسی",
                        subtitle = "رابط راست‌به‌چپ • فونت فارسی وزیرمتن",
                        badgeText = "FA",
                        fontFamily = VazirmatnFamily,
                        cardDirection = LayoutDirection.Rtl,
                        selected = selectedLanguage == AppLanguage.FA,
                        onClick = { onSelectLanguage(AppLanguage.FA) },
                        modifier = Modifier.testTag("language_option_fa")
                    )

                    LanguageChoiceCard(
                        title = "English",
                        subtitle = "Left-to-Right • English Interface",
                        badgeText = "EN",
                        fontFamily = PlusJakartaSansFamily,
                        cardDirection = LayoutDirection.Ltr,
                        selected = selectedLanguage == AppLanguage.EN,
                        onClick = { onSelectLanguage(AppLanguage.EN) },
                        modifier = Modifier.testTag("language_option_en")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Uniform Opaque Reassurance Banner
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceContainerLow,
                    border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isFa) {
                                "انتخاب شما ذخیره می‌شود و در ورودهای بعدی مستقیماً با همین زبان وارد می‌شوید"
                            } else {
                                "Your choice is saved so future launches open directly in your language"
                            },
                            fontFamily = if (isFa) VazirmatnFamily else PlusJakartaSansFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            lineHeight = if (isFa) 21.sp else 18.sp,
                            color = OnPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Seamless Bottom Confirm CTA (same Surface color, zero shadow seam)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .navigationBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = { onConfirmLanguage(selectedLanguage) },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryContainer,
                            contentColor = OnPrimary
                        ),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("language_confirm_button")
                    ) {
                        Text(
                            text = if (isFa) "ادامه با زبان فارسی" else "Continue in English",
                            fontFamily = if (isFa) VazirmatnFamily else PlusJakartaSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageChoiceCard(
    title: String,
    subtitle: String,
    badgeText: String,
    fontFamily: FontFamily,
    cardDirection: LayoutDirection,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) SelectedCardSurface else SurfaceContainerLow,
        animationSpec = tween(durationMillis = 200),
        label = "lang_card_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) PrimaryContainer else OutlineVariant.copy(alpha = 0.55f),
        animationSpec = tween(durationMillis = 200),
        label = "lang_card_border"
    )

    CompositionLocalProvider(LocalLayoutDirection provides cardDirection) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(22.dp),
            color = containerColor,
            border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            modifier = modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Language Code Badge
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) PrimaryContainer else SurfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = if (selected) OnPrimary else Primary
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = title,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        lineHeight = 28.sp,
                        color = OnSurface
                    )
                    Text(
                        text = subtitle,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        color = if (selected) OnPrimaryContainer else OnSurfaceVariant
                    )
                }

                // Selection Indicator Circle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (selected) PrimaryContainer else SurfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = if (cardDirection == LayoutDirection.Rtl) "انتخاب‌شده" else "Selected",
                            tint = OnPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
