# GitHub Actions CI/CD & Automated APK Releases

This repository includes standard GitHub Actions workflows for continuous integration, automated builds, and publishing APK releases directly to GitHub Releases.

---

## 🛠️ Workflows Included

### 1. **Release Workflow** (`.github/workflows/release.yml`)
- **Triggers**:
  - Automatically runs whenever a Git version tag is pushed: `git tag v1.0.0 && git push origin v1.0.0`
  - Can be manually triggered via **GitHub Actions tab** → **Build & Release APK** (`workflow_dispatch`) with custom tag names, release titles, and draft/pre-release flags.
- **Actions**:
  - Sets up Java 17 (Temurin) & Gradle cache.
  - Decodes signing keystores (or uses fallback signature).
  - Compiles `:app:assembleRelease` and `:app:assembleDebug`.
  - Generates SHA256 checksums (`SHA256SUMS.txt`).
  - Publishes a **GitHub Release** with:
    - Production APK: `HermesMobile-vX.Y.Z-release.apk`
    - Debug APK: `HermesMobile-vX.Y.Z-debug.apk`
    - Checksums: `SHA256SUMS.txt`
    - Auto-generated release changelog.

### 2. **Continuous Integration (CI) Workflow** (`.github/workflows/ci.yml`)
- **Triggers**: On pull requests and pushes to `main` / `master` / `develop`.
- **Actions**:
  - Runs local unit tests (`gradle :app:testDebugUnitTest`).
  - Verifies debug compilation (`gradle :app:assembleDebug`).
  - Uploads debug APK to workflow artifacts.

---

## 🔐 Optional GitHub Repository Secrets

Configure these under **Repository Settings** → **Secrets and variables** → **Actions**:

| Secret Name | Description | Default / Fallback |
| :--- | :--- | :--- |
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded production `.jks` keystore | Generated or debug keystore fallback |
| `STORE_PASSWORD` | Password for the release keystore | `android` |
| `KEY_PASSWORD` | Password for the release key alias | `android` |
| `GEMINI_API_KEY` | Google AI Studio Gemini API Key | Uses `.env.example` / user runtime input |

### How to generate `RELEASE_KEYSTORE_BASE64`:
```bash
base64 -w 0 my-upload-key.jks
```
Copy the output string and paste it into the `RELEASE_KEYSTORE_BASE64` secret.

---

## 🚀 How to Publish a New Release

### Option A: Via Git Tags (Recommended)
```bash
git checkout main
git pull
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

### Option B: Via GitHub Web Interface
1. Go to your repository on GitHub.
2. Click on the **Actions** tab.
3. Select **Build & Release APK** on the left.
4. Click **Run workflow**, enter the version tag (e.g. `v1.0.0`), and submit.
