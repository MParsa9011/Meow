# Contributing to Meow

Thank you for your interest in contributing to **Meow — Cat Voice Translator**! We welcome community contributions ranging from bug fixes and bioacoustic feature engineering to UI enhancements and localization.

---

## Code of Conduct

We are committed to providing a friendly, safe, and welcoming environment for all contributors. Please treat fellow community members with respect, patience, and kindness.

---

## Development Workflow

### 1. Prerequisites
- **Java Development Kit (JDK)**: Version 21 (Temurin or OpenJDK recommended)
- **Android Studio**: Ladybug / Meerkat (2024.2+ recommended)
- **Android SDK**: Compile SDK 36, Minimum SDK 26 (Android 8.0 Oreo)
- **Gradle**: Version 9.3.1 (or managed through Android Studio)

### 2. Fork & Clone
```bash
git clone https://github.com/MParsa9011/Meow.git
cd Meow
```

### 3. Create a Feature Branch
Create a descriptive branch from `main`:
```bash
git checkout -b feat/your-feature-name
# or
git checkout -b fix/issue-description
```

### 4. Local Configuration
Copy the sample environment file:
```bash
cp .env.example .env
```
For local debug runs, you do not need a production Myket billing key. The default placeholder in `.env.example` allows compiling and running all core features.

### 5. Build & Verify Locally
Verify that the project builds cleanly before committing:
```bash
# Generate local debug keystore if needed
keytool -genkey -v -keystore debug.keystore \
  -storepass android -alias androiddebugkey -keypass android \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=Android Debug,O=Android,C=US"

# Compile debug APK
gradle assembleDebug
```

---

## Commit Message Guidelines

We follow the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) specification:

- `feat:` A new feature for the application or AI pipeline
- `fix:` A bug fix
- `docs:` Documentation updates or additions
- `refactor:` Code changes that neither fix a bug nor add a feature
- `perf:` Performance improvements (e.g. DSP optimization, inference speedup)
- `chore:` Build scripts, dependencies, or maintenance tasks
- `ci:` Changes to GitHub Actions workflows

**Example:**
```bash
git commit -m "feat(audio): optimize YIN pitch tracking window for high-register kitten meows"
```

---

## Coding Standards

- **Kotlin & Compose Idioms**: Use idiomatic Kotlin, avoid unnecessary mutable state, and structure UI components cleanly with `@Composable` functions.
- **Material 3**: Follow Material Design 3 guidelines and respect dynamic theming (Dark & Light).
- **Architecture**: Adhere to MVVM architecture:
  - UI layer (`ui/screens`, `ui/components`, `ui/modals`)
  - ViewModel layer (`ui/MainViewModel.kt`)
  - Domain & DSP layer (`audio/AudioDsp.kt`, `audio/CatAiInferenceEngine.kt`)
  - Data layer (`data/local`, `data/model`)
- **No Secrets**: Never commit private keys, keystores, or production credentials.

---

## Submitting a Pull Request

1. Push your branch to your GitHub fork:
   ```bash
   git push origin feat/your-feature-name
   ```
2. Open a Pull Request against the `main` branch of `MParsa9011/Meow`.
3. Complete the pull request template checklist describing your changes, motivation, and verification steps.
4. Ensure all GitHub Actions CI checks pass.
