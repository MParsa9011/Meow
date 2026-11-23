# Changelog

All notable changes to the Meow Cat Voice Translator project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned
- Continuous background listening mode with wake-word activation.
- Export bioacoustic pipeline to standalone ONNX / TensorFlow Lite models.
- Wear OS companion application for wrist-based translation alerts.
- Cat voice personalization and individual calibration profiles.

## [2.4.1] - 2026-11-23

### Added
- First-run language selection screen supporting English and Persian (فارسی).
- Persistent language preference stored in local datastore with immediate RTL layout mirroring.
- Enhanced CI pipeline workflow with automated debug keystore generation and build checks.
- Community standards documentation including contribution guide, issue templates, and MIT license.

## [2.4.0] - 2026-11-19

### Added
- On-device bioacoustic feline AI inference engine with 3-class intent matching (`Brushing`, `Food`, `Isolation`).
- Dual-branch classification architecture:
  - 234-feature acoustic pipeline (YIN F0, MFCCs, spectral contrast, spectral flux).
  - 128-bin Mel Spectrogram 2D Convolutional Neural Network (CNN).
  - Late-fusion blending achieving 79.4% test accuracy.
- Audio DSP module supporting 8 kHz linear resampling, DC offset elimination, 70 Hz Butterworth high-pass filtering, and silence trimming.
- Live microphone recording manager using Android `AudioRecord`.
- Bundled feline vocalization samples for instant offline demonstration.
- Full Jetpack Compose & Material 3 UI with dark/light themes.
- Room database integration for translation records, feedback logs, and multi-pet management.
- CSV export for recorded audio analysis using Android `FileProvider`.
- Myket In-App Billing (IAB v3) integration with cryptographic RSA verification.
