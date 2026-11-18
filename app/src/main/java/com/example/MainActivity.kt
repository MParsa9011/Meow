package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.PetProfile
import com.example.ui.MainViewModel
import com.example.ui.RecordingPhase
import com.example.ui.components.FloatingToastBanner
import com.example.ui.components.MeowBottomBar
import com.example.ui.components.MeowTab
import com.example.ui.components.MeowTopBar
import com.example.ui.modals.AddOrEditPetBottomSheet
import com.example.ui.modals.HowAiWorksDialog
import com.example.ui.modals.MeowProBottomSheet
import com.example.ui.modals.PrivacyPolicyDialog
import com.example.ui.modals.SwitchPetBottomSheet
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TranslateScreen
import com.example.ui.theme.MeowTheme
import com.example.ui.theme.Surface

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as MeowApplication
        MainViewModel.Factory(app.repository, app.aiEngine, app.audioManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MeowTheme {
                val context = LocalContext.current
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route ?: MeowTab.TRANSLATE.route

                val pets by viewModel.pets.collectAsStateWithLifecycle()
                val activePet by viewModel.activePet.collectAsStateWithLifecycle()
                val translations by viewModel.translations.collectAsStateWithLifecycle()
                val feedbackLogs by viewModel.feedbackLogs.collectAsStateWithLifecycle()
                val settings by viewModel.settings.collectAsStateWithLifecycle()
                val translatorState by viewModel.translatorState.collectAsStateWithLifecycle()
                val liveAmplitudes by viewModel.liveAmplitudes.collectAsStateWithLifecycle()
                val playingAudioId by viewModel.playingAudioId.collectAsStateWithLifecycle()
                val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

                var hasMicPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                var showSwitchPetSheet by remember { mutableStateOf(false) }
                var showAddEditPetSheet by remember { mutableStateOf(false) }
                var petBeingEdited by remember { mutableStateOf<PetProfile?>(null) }
                var showProSheet by remember { mutableStateOf(false) }
                var showHowAiDialog by remember { mutableStateOf(false) }
                var showPrivacyDialog by remember { mutableStateOf(false) }

                val micPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    hasMicPermission = granted
                    if (granted) {
                        viewModel.startRecording()
                    } else {
                        viewModel.showToast("Microphone permission is required to record live cat meows")
                    }
                }

                val audioPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        viewModel.analyzeUploadedUri(uri)
                    }
                }

                val photoPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null) {
                        viewModel.updatePetCustomAvatar(uri)
                    }
                }

                val subtitle = when (currentRoute) {
                    MeowTab.HISTORY.route -> "Translation History"
                    MeowTab.PROFILE.route -> "${activePet?.name ?: "Cat"} Profile"
                    MeowTab.SETTINGS.route -> "AI & Audio Settings"
                    else -> "Cat Voice Translator"
                }

                val navigateTab: (MeowTab) -> Unit = { tab ->
                    if (currentRoute != tab.route) {
                        navController.navigate(tab.route) {
                            popUpTo(MeowTab.TRANSLATE.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }

                Scaffold(
                    containerColor = Surface,
                    topBar = {
                        MeowTopBar(
                            subtitle = subtitle,
                            activePet = activePet,
                            isPro = settings.isProMember,
                            onSettingsClick = { navigateTab(MeowTab.SETTINGS) },
                            onProfileClick = { navigateTab(MeowTab.PROFILE) }
                        )
                    },
                    bottomBar = {
                        MeowBottomBar(
                            currentRoute = currentRoute,
                            onNavigate = navigateTab
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = MeowTab.TRANSLATE.route,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(MeowTab.TRANSLATE.route) {
                                TranslateScreen(
                                    activePet = activePet,
                                    uiState = translatorState,
                                    settings = settings,
                                    liveAmplitudes = liveAmplitudes,
                                    playingAudioId = playingAudioId,
                                    bundledSamples = viewModel.bundledSamples,
                                    hasMicPermission = hasMicPermission,
                                    onToggleRecording = {
                                        if (translatorState.phase == RecordingPhase.RECORDING) {
                                            viewModel.stopRecordingAndAnalyze()
                                        } else if (hasMicPermission) {
                                            viewModel.startRecording()
                                        } else {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    onUploadAudio = {
                                        audioPickerLauncher.launch(arrayOf("audio/*"))
                                    },
                                    onSelectSample = { sample ->
                                        viewModel.analyzeBundledSample(sample)
                                    },
                                    onSelectMatchRank = { rank ->
                                        viewModel.selectExpandedMatch(rank)
                                    },
                                    onPlayCatResponse = { playId, code ->
                                        viewModel.playCatResponse(playId, code)
                                    },
                                    onPlayAudioClip = { playId, path, asset ->
                                        viewModel.playRecordedOrSample(playId, path, asset)
                                    },
                                    onToggleBookmark = {
                                        viewModel.toggleCurrentBookmark()
                                    },
                                    onShareResult = { match ->
                                        val shareText = buildString {
                                            append("${activePet?.name ?: "Cat"}'s Meow Translation: ")
                                            append("${match.title} (${match.percentage}% match)")
                                            if (settings.showPersianSubtitles) {
                                                append(" — ${match.titleFa}")
                                            }
                                            append("\nPitch: ${match.pitchHz} • Duration: ${match.durationSec}")
                                            append("\n${match.description}")
                                        }
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(intent, "Share Cat Translation")
                                        )
                                    },
                                    onSubmitFeedback = { verdict, corrected ->
                                        viewModel.submitFeedback(verdict, corrected)
                                    },
                                    onOpenSwitchPet = { showSwitchPetSheet = true },
                                    onOpenModelSelector = { navigateTab(MeowTab.SETTINGS) }
                                )
                            }

                            composable(MeowTab.HISTORY.route) {
                                HistoryScreen(
                                    translations = translations,
                                    playingAudioId = playingAudioId,
                                    showPersian = settings.showPersianSubtitles,
                                    onPlayAudio = { record ->
                                        if (record.audioPath != null || record.sampleAssetName != null) {
                                            viewModel.playRecordedOrSample(
                                                "hist_play_${record.id}",
                                                record.audioPath,
                                                record.sampleAssetName
                                            )
                                        } else {
                                            viewModel.playCatResponse(
                                                "hist_play_${record.id}",
                                                com.example.data.model.IntentCode.fromCode(record.intentCode)
                                            )
                                        }
                                    },
                                    onToggleBookmark = { record ->
                                        viewModel.toggleHistoryBookmark(record)
                                    },
                                    onSubmitFeedback = { record, verdict, corrected ->
                                        viewModel.submitHistoryFeedback(record, verdict, corrected)
                                    },
                                    onDeleteRecord = { id ->
                                        viewModel.deleteHistoryRecord(id)
                                    },
                                    onRecordNewMeow = {
                                        navigateTab(MeowTab.TRANSLATE)
                                    }
                                )
                            }

                            composable(MeowTab.PROFILE.route) {
                                ProfileScreen(
                                    activePet = activePet,
                                    translations = translations,
                                    onPickPhoto = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    onEditProfile = {
                                        petBeingEdited = activePet
                                        showAddEditPetSheet = true
                                    },
                                    onSwitchPet = { showSwitchPetSheet = true },
                                    onAddPet = {
                                        petBeingEdited = null
                                        showAddEditPetSheet = true
                                    },
                                    onShowToast = { msg -> viewModel.showToast(msg) }
                                )
                            }

                            composable(MeowTab.SETTINGS.route) {
                                SettingsScreen(
                                    activePet = activePet,
                                    settings = settings,
                                    feedbackCount = feedbackLogs.size,
                                    onSelectEngine = { engine -> viewModel.setModelEngine(engine) },
                                    onUpdateSettings = { transform -> viewModel.updateSettings(transform) },
                                    onSwitchPet = { showSwitchPetSheet = true },
                                    onExportHistory = { viewModel.exportHistory(context) },
                                    onClearHistoryAndCache = { viewModel.clearHistoryAndCache() },
                                    onOpenProModal = { showProSheet = true },
                                    onOpenHowAiWorks = { showHowAiDialog = true },
                                    onOpenPrivacyPolicy = { showPrivacyDialog = true }
                                )
                            }
                        }

                        FloatingToastBanner(
                            message = toastMessage,
                            onDismiss = { viewModel.dismissToast() },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp)
                        )
                    }
                }

                if (showSwitchPetSheet) {
                    SwitchPetBottomSheet(
                        pets = pets,
                        onSelectPet = { id -> viewModel.switchActivePet(id) },
                        onAddNewPet = {
                            petBeingEdited = null
                            showAddEditPetSheet = true
                        },
                        onDismiss = { showSwitchPetSheet = false }
                    )
                }

                if (showAddEditPetSheet) {
                    AddOrEditPetBottomSheet(
                        existingPet = petBeingEdited,
                        onSavePet = { pet, isNew -> viewModel.savePet(pet, isNew) },
                        onDismiss = { showAddEditPetSheet = false }
                    )
                }

                if (showProSheet) {
                    MeowProBottomSheet(
                        isProMember = settings.isProMember,
                        onActivateProPlaceholder = {
                            viewModel.updateSettings { it.copy(isProMember = !it.isProMember) }
                            viewModel.showToast("Meow Pro preview toggled (TODO: Google Play Billing)")
                        },
                        onDismiss = { showProSheet = false }
                    )
                }

                if (showHowAiDialog) {
                    HowAiWorksDialog(onDismiss = { showHowAiDialog = false })
                }

                if (showPrivacyDialog) {
                    PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
                }
            }
        }
    }
}
