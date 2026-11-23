---
name: Bug Report
about: Create a report to help us improve the Meow Android app or bioacoustic inference engine
title: "[BUG] "
labels: ["bug"]
assignees: ""
---

### Problem Description
A clear and concise description of the bug encountered.

### Steps to Reproduce
1. Open the Meow app.
2. Navigate to screen (e.g. Translate / History / Cat Profile / Settings).
3. Perform action (e.g. tap live recording, upload audio sample, or submit feedback).
4. See unexpected error or behavior.

### Expected Behavior
A clear and concise description of what you expected to happen.

### Audio & Inference Details (If Applicable)
- **Audio Source**: Live microphone / Uploaded WAV / Bundled sample
- **Inference Engine**: Hybrid Late Fusion / Bioacoustic Pipeline / Spectrogram CNN
- **Cat Intent Detected**: Brushing / Food / Isolation / None

### Device & OS Environment
- **Device Model**: (e.g., Google Pixel 8, Samsung Galaxy S23, Xiaomi 13)
- **Android Version**: (e.g., Android 14, Android 13, API level)
- **App Version / Commit**: (e.g., v2.4.1 or commit SHA)

### Relevant Logs / Screenshots
Attach Logcat output (`adb logcat -s CatAiInferenceEngine AudioRecorderManager MainViewModel`) or relevant screenshots.
