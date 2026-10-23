# Hermit Release & Update System

Hermit features a secure, privacy-preserving, and lightweight update mechanism that distributes verified APKs directly through [public GitHub Releases](https://github.com/mr-umar/Hermit/releases).

This system operates without proprietary update services (Google Play Services, Firebase App Distribution, etc.), requires no user account or GitHub token, and adheres strictly to Android security controls.

---

## 1. Architecture & Design Principles

### Public GitHub Releases API
- **Endpoint**: `https://api.github.com/repos/mr-umar/Hermit/releases`
- **Authentication**: None required. Uses standard HTTP `GET` with `Accept: application/vnd.github+json` and a descriptive `User-Agent`.
- **Filtering**: Automatically ignores draft releases and prereleases. Only official stable releases are eligible for updates.

### Semantic Versioning (SemVer 2.0.0)
- Version comparison uses numeric SemVer precedence (`MAJOR.MINOR.PATCH`) instead of naive lexicographical string comparison.
- Properly handles:
  - Multi-digit numbers (e.g., `1.10.0` is strictly newer than `1.9.0`).
  - Leading `v` prefixes (e.g., `v1.0.1` and `1.0.1` parse identically).
  - Pre-release identifiers and build metadata.

### Privacy & Polling Policy
- **No Background Polling**: Hermit does not run persistent background jobs or services to check for updates.
- **Manual Checks**: Users can check for updates on demand via **Settings > Updates > Check for updates**.
- **Optional Launch Check**: Users can opt in to a lightweight check when opening the app (**Settings > Updates > Check on app launch**), throttled to at most once every 6 hours.

---

## 2. Security & Verification Pipeline

Hermit implements defense-in-depth before handing any package to the Android installer:

```
[GitHub Release Asset]
        │
        ▼ (Download via HttpURLConnection with progress & cancellation)
[Cached APK in app cache /updates/]
        │
        ▼ Step 1: Checksum Integrity Check
[SHA-256 Calculation] ──► Verified against SHA256SUMS.txt or Release Notes
        │
        ▼ Step 2: Pre-Installation Package Security Check
[ApkSecurityVerifier]
        ├─ Package Name == "xyz.umarmohammad.hermit"
        ├─ Version Code >= Installed Version Code (No Downgrade)
        └─ Signing Certificate SHA-256 Fingerprint == Installed App Certificate
        │
        ▼ Step 3: Official Android Package Installer
[FileProvider URI] ──► Intent(ACTION_VIEW) ──► Android OS Package Installer
```

### Step 1: SHA-256 Integrity Verification
- Downloads the release APK alongside `SHA256SUMS.txt` (or parses the checksum from the release markdown body).
- Computes the SHA-256 hash using streaming `MessageDigest` without holding the entire file in RAM.
- **Integrity vs. Authenticity**: SHA-256 is treated as an *integrity* check against transmission errors and file corruption. Proof of authenticity relies on digital cryptographic signatures.

### Step 2: Signature & Package Authenticity Check
Before invoking Android's installer, `ApkSecurityVerifier` inspects the downloaded archive:
1. **Application ID Match**: Validates that the APK package identifier matches `xyz.umarmohammad.hermit`.
2. **Version Monotonicity**: Verifies that the APK version code is greater than or equal to the currently installed version code, preventing downgrade attacks.
3. **Certificate Fingerprint Match**: Extracts the SHA-256 fingerprint of the signing certificate embedded in the downloaded APK (supporting both modern Android P+ `signingInfo` and legacy `signatures`) and verifies that it is identical to the certificate signing the running application.

If any check fails, the cached file is immediately deleted, the update is aborted, and an informative, localized error is displayed to the user.

### Step 3: Supported Android Installer Flow
- **No Silent Installs**: Hermit never executes shell commands or bypasses Android's package manager.
- **Permissions**:
  - `android.permission.INTERNET`: Required to check releases and download assets.
  - `android.permission.REQUEST_INSTALL_PACKAGES`: On Android 8.0+ (API 26), allows requesting installation of verified packages.
- **Permission Flow**: If `canRequestPackageInstalls()` returns `false`, Hermit presents a clear educational dialog explaining why the permission is required and navigates the user to `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES`.
- **Handoff**: The verified APK is exposed securely via AndroidX `FileProvider` (`content://xyz.umarmohammad.hermit.fileprovider/updates/...`) with temporary read permissions. The official Android OS installer asks for user confirmation and performs its own kernel-level signature validation before installation.

---

## 3. Release Artifact Naming Convention

All Hermit release assets must follow this naming convention:

| Asset Name | Description | Example |
|---|---|---|
| `Hermit-v<MAJOR>.<MINOR>.<PATCH>.apk` | Production signed release APK | `Hermit-v1.0.1.apk` |
| `SHA256SUMS.txt` | Text file with SHA-256 checksums | See format below |

### Format of `SHA256SUMS.txt`
The file must contain standard Unix `sha256sum` output:
```text
<64-hex-sha256-hash>  Hermit-v1.0.1.apk
```

---

## 4. Release Signing Key Setup

> [!IMPORTANT]
> The same signing key must be preserved across all application updates. If the signing key is lost or modified, existing users will not be able to install updates without completely uninstalling the app and losing their local data.

### Generating a Production Keystore
To generate a secure, 4096-bit RSA release signing key:

```bash
keytool -genkeypair \
  -v \
  -keystore hermit-release.jks \
  -alias hermit \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -storetype PKCS12
```

Convert the keystore file to base64 for GitHub Secrets storage:

```bash
# On Linux / macOS:
base64 -w 0 hermit-release.jks > keystore_base64.txt

# On Windows (PowerShell):
[Convert]::ToBase64String([IO.File]::ReadAllBytes("hermit-release.jks")) | Set-Content keystore_base64.txt
```

### GitHub Repository Secrets Configuration

Navigate to your GitHub repository: **Settings > Secrets and variables > Actions > New repository secret**.

Configure the following secrets:

| Secret Name | Description | Example / Format |
|---|---|---|
| `KEYSTORE_BASE64` | Base64-encoded content of `hermit-release.jks` | Output of base64 command |
| `KEYSTORE_PASSWORD` | Password protecting the keystore file | Keystore password |
| `KEY_ALIAS` | Alias of the release signing key | `hermit` |
| `KEY_PASSWORD` | Password protecting the private key | Key password |

> [!WARNING]
> Never commit keystore files (`.jks`, `.keystore`), passwords, or secrets to Git.

---

## 5. GitHub Actions Release Workflow

The workflow at `.github/workflows/release.yml` automatically executes whenever a version tag matching `v*` is pushed to GitHub.

### Security Gating
If the release signing secrets are not configured in repository settings, the workflow **fails with an informative error** and refuses to publish an unsigned or debug APK. Production publication remains gated until signing credentials are provided.

### Workflow Pipeline
1. Validates the Gradle wrapper checksum.
2. Checks for required GitHub Actions secrets.
3. Decodes the keystore to a temporary runner path.
4. Executes unit tests (`testDebugUnitTest`).
5. Builds the production signed APK via `./gradlew assembleRelease`.
6. Renames the artifact to `Hermit-v<TAG>.apk`.
7. Computes SHA-256 and generates `SHA256SUMS.txt`.
8. Securely shreds and deletes the temporary keystore file.
9. Publishes the release using `softprops/action-gh-release@v2`.

---

## 6. Creating a New Release

To release a new version of Hermit:

1. **Update Version in `app/build.gradle.kts`**:
   ```kotlin
   versionCode = 2
   versionName = "1.0.1"
   ```

2. **Commit and Tag**:
   ```bash
   git add app/build.gradle.kts
   git commit -m "Bump version to v1.0.1"
   git tag -a v1.0.1 -m "Release v1.0.1: [Summary of changes]"
   ```

3. **Push Tag to GitHub**:
   ```bash
   git push origin main
   git push origin v1.0.1
   ```

4. **Monitor GitHub Actions**:
   - Go to the **Actions** tab on GitHub.
   - The `Release APK` workflow will build, sign, verify, and publish the release.
   - Installed Hermit applications will discover the release via **Settings > Updates**.
