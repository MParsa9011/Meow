# Meow — Cat Voice Translator (Android)

An on-device bioacoustic AI Android application built with Kotlin, Jetpack Compose, Material 3, and Room.

## Features

- **On-Device Cat Voice AI Engine**:
  - **Hybrid Late Fusion AI (79.4% Acc)**: Blends the 234-feature Enhanced Bioacoustic Pipeline (`45%`) and 128-bin Mel Spectrogram 2D CNN (`55%`) using trained weights bundled in `app/src/main/assets/cat_ai_model.json`.
  - **Audio DSP Preprocessing**: DC offset removal, 70 Hz 2nd-order Butterworth high-pass filtering, silence trimming, and YIN pitch (`F0` Hz) extraction.
  - **Live Recording, File Upload & Bundled Samples**: Record live meows via `AudioRecord` (`RECORD_AUDIO`), import audio files via `OpenDocument`, or test directly with bundled feline dataset samples.
  - **Bilingual Intent Matching & Active Learning**: Ranked intent cards (`Affection & Brushing Care`, `Hungry & Wants Food!`, `Isolation, Calling or Distress`) with optional Persian (`Vazirmatn`) subtitles, synthesized purr/chirp playback, and Yes/No + tag-correction feedback logs stored in Room.
- **Translation History**: Weekly vocal summary banner, search bar, time horizon & intent category filters, waveform playback, bookmarking, inline tag correction, and CSV export via `FileProvider`.
- **Cat Profiles**: Multi-cat switching, custom photo portraits via Android Photo Picker (`PickVisualMedia`), acoustic calibration metrics, weekly vocal trends, and health/vet info.
- **AI & Audio Settings**: Inference model selector, sensitivity/quality/DSP controls, notification preferences, cache management, and a Meow Pro preview modal.
