### Description
Provide a concise explanation of the changes introduced in this pull request and link any relevant issue tickets.

### Type of Change
- [ ] Bug fix (non-breaking change fixing an issue)
- [ ] New feature (non-breaking change adding functionality)
- [ ] Breaking change (fix or feature causing existing behavior to change)
- [ ] Performance optimization / DSP tuning
- [ ] UI / Jetpack Compose redesign
- [ ] Documentation / CI / Tooling update

### Affected Subsystems
- [ ] UI Screens / Modals (`ui/screens`, `ui/modals`, `ui/components`)
- [ ] Audio DSP & Recording (`audio/AudioDsp.kt`, `audio/AudioRecorderManager.kt`)
- [ ] AI Inference Engine (`audio/CatAiInferenceEngine.kt`, `assets/cat_ai_model.json`)
- [ ] Local Storage & Database (`data/local/MeowDatabase.kt`, Room entities)
- [ ] Billing & Security (`billing/MyketIabHelper.kt`, `billing/MyketSecurity.kt`)
- [ ] CI / CD / Build scripts

### Testing Performed
- [ ] Compiled cleanly with Gradle (`gradle assembleDebug`)
- [ ] Verified on Android device or emulator (API level: _____)
- [ ] Tested live audio recording and playback
- [ ] Tested bundled sample inference (`F_MIN01`, `B_BRA01`, `I_MIN01`)
- [ ] Verified RTL / Persian localization display

### Checklist
- [ ] My code follows the repository's Kotlin & Compose code style conventions.
- [ ] I have commented complex DSP math or inference logic where appropriate.
- [ ] I have verified that no sensitive credentials or keys are committed.
