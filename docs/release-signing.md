# Hermit Release Signing & Key Management

This guide documents the production signing architecture for Hermit, how to generate and protect your private release key, how Gradle builds signed release artifacts, and how to verify APK signatures.

---

## 1. Release Signing Identity & Principles

- **Ownership & Control**: You own and maintain the private release key. No private key material is ever committed to Git, stored in tracked files, or uploaded to unencrypted cloud storage.
- **Strict Separation**: Debug builds always use standard Android debug certificates; production release builds use your dedicated production key.
- **No Silent Fallback**: If release credentials are not configured, Gradle builds an unsigned release APK and alerts you explicitly; it never silently signs a release build with a debug key.
- **Update Integrity**: Android's package manager and Hermit's internal updater require the signing certificate of an update to match the installed app's certificate. Changing or losing the key prevents users from installing updates without wiping their app data.

---

## 2. Generating the Production Release Key

The release key must be generated locally in a dedicated directory **outside** the Git repository.

### Directory Location
We recommend using:
- **Windows**: `C:\Users\<username>\.hermit-signing\` (or `$HOME\.hermit-signing\`)
- **Linux / macOS**: `~/.hermit-signing/`

### Generation Command
Open your terminal (PowerShell, Command Prompt, or Bash) and execute `keytool`:

```bash
keytool -genkeypair \
  -v \
  -keystore "$HOME/.hermit-signing/hermit-release.jks" \
  -alias hermit \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -storetype PKCS12 \
  -dname "CN=Hermit, OU=Mobile, O=Hermit, C=ES"
```

> [!NOTE]
> On Windows PowerShell, `$HOME\.hermit-signing\hermit-release.jks` resolves to `C:\Users\<username>\.hermit-signing\hermit-release.jks`.

### Why Run This Manually in Your Terminal?
`keytool` interactively prompts for the keystore password with masked input. Running this directly in your terminal ensures your password is never passed as a command-line argument, never logged in process tables or history files, and never transmitted to an external service.

---

## 3. Configuring Local Credentials

Hermit supports two secure methods for supplying signing credentials to Gradle without committing secrets to Git.

### Option A: Environment Variables (Recommended for Shell & CI)

Set the following environment variables in your terminal session or shell profile:

#### Windows (PowerShell):
```powershell
$env:HERMIT_KEYSTORE_PATH = "$HOME\.hermit-signing\hermit-release.jks"
$env:HERMIT_KEYSTORE_PASSWORD = "your_strong_password"
$env:HERMIT_KEY_ALIAS = "hermit"
$env:HERMIT_KEY_PASSWORD = "your_strong_password"
```

#### Linux / macOS (Bash / Zsh):
```bash
export HERMIT_KEYSTORE_PATH="$HOME/.hermit-signing/hermit-release.jks"
export HERMIT_KEYSTORE_PASSWORD="your_strong_password"
export HERMIT_KEY_ALIAS="hermit"
export HERMIT_KEY_PASSWORD="your_strong_password"
```

---

### Option B: Local Properties File (`signing.properties`)

You can create an untracked properties file in the project root (`signing.properties` or `local.signing.properties`) or in `~/.hermit-signing/signing.properties`.

1. Copy the provided template:
   ```bash
   cp signing.properties.example signing.properties
   ```
2. Edit `signing.properties` with your local values:
   ```properties
   HERMIT_KEYSTORE_PATH=C:/Users/YourUsername/.hermit-signing/hermit-release.jks
   HERMIT_KEYSTORE_PASSWORD=your_keystore_password
   HERMIT_KEY_ALIAS=hermit
   HERMIT_KEY_PASSWORD=your_key_password
   ```

> [!IMPORTANT]
> The project root `.gitignore` is pre-configured to ignore `signing.properties`, `local.signing.properties`, and all `*.jks`/`*.keystore` files. Never force-add these files to Git (`git add -f`).

---

## 4. Building a Signed Release APK

Once your credentials are configured via environment variables or `signing.properties`:

```bash
# Windows
.\gradlew.bat assembleRelease

# Linux / macOS
./gradlew assembleRelease
```

The output APK will be generated at:
`app/build/outputs/apk/release/app-release.apk`

If credentials are **not** present, Gradle will print:
`NOTICE: Release signing is not configured (HERMIT_KEYSTORE_PATH is not set). Building unsigned release APK.`
The output will be named `app-release-unsigned.apk`.

---

## 5. Verifying Signatures with `apksigner`

Android SDK includes `apksigner` inside `build-tools/<version>/`. You can verify that the release APK is properly signed using modern v2/v3 signing schemes:

```bash
# Windows
& "C:\Users\<username>\AppData\Local\Android\Sdk\build-tools\34.0.0\apksigner.bat" verify --verbose --print-certs app\build\outputs\apk\release\app-release.apk

# Linux / macOS
$ANDROID_HOME/build-tools/34.0.0/apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
```

### Expected Output for a Signed Release
```text
Verifies
Verified using v1 scheme (JAR signing): false
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): true
Number of signers: 1
Signer #1 certificate DN: CN=Hermit, OU=Mobile, O=Hermit, C=ES
Signer #1 certificate SHA-256 digest: <64-character-hex-fingerprint>
```

Confirm that the application ID matches `xyz.umarmohammad.hermit` using `aapt2`:
```bash
aapt2 dump badging app/build/outputs/apk/release/app-release.apk | Select-String "package: name"
```

---

## 6. Inspecting Certificate Fingerprints

To inspect the certificate fingerprint directly from the keystore before building:

```bash
keytool -list -v \
  -keystore "$HOME/.hermit-signing/hermit-release.jks" \
  -alias hermit
```

Look for the **Certificate fingerprints** section:
```text
Certificate fingerprints:
     SHA1:   XX:XX:XX:...
     SHA256: YY:YY:YY:...
```
Save your SHA-256 fingerprint in a safe reference note. This fingerprint uniquely identifies your release builds.

---

## 7. Backup and Disaster Recovery

### Safe Backup Practices
1. **Cold Storage / Offline Backup**:
   - Store a copy of `hermit-release.jks` on an encrypted USB flash drive (BitLocker, LUKS, or VeraCrypt).
   - Store the password in an encrypted password manager (1Password, Bitwarden, KeePassXC).
2. **Never Store Unencrypted in Cloud**:
   - Do NOT upload `hermit-release.jks` unencrypted to Google Drive, Dropbox, OneDrive, iCloud, or public repositories.
3. **Recovery Procedure**:
   - On a new workstation, copy `hermit-release.jks` to `~/.hermit-signing/hermit-release.jks`.
   - Set the environment variables or `signing.properties`.
   - Run `./gradlew assembleRelease`.

---

## 8. Why Preserving the Key is Critical

Android enforces the **Same-Origin Signature Policy**:
- An installed application can only be upgraded by an APK signed with the exact same certificate.
- If you lose the key or generate a new one with a different certificate, users attempting to update Hermit will receive:
  `INSTALL_FAILED_UPDATE_INCOMPATIBLE`
- In addition, Hermit's internal update verifier (`ApkSecurityVerifier`) inspects the signature before invoking the OS installer and will reject any APK whose signing certificate does not match the currently running app.
- If the key is lost, users would be forced to uninstall Hermit and reinstall from scratch, permanently wiping their profiles, NFC tag mappings, and focus statistics.

---

## 9. Credential Rotation (Without Breaking Updates)

If a password is compromised or needs updating, you can rotate the **passwords** without modifying the underlying private key or certificate. Existing installations will continue updating without issue.

### Change Keystore Password
```bash
keytool -storepasswd \
  -keystore "$HOME/.hermit-signing/hermit-release.jks"
```

### Change Key Entry Password
```bash
keytool -keypasswd \
  -keystore "$HOME/.hermit-signing/hermit-release.jks" \
  -alias hermit
```

---

## 10. GitHub Releases vs. F-Droid Signing

It is important to understand the distinction between release channels in open-source Android software:

| Channel | Who Builds It? | Who Signs It? | Signature Compatibility |
|---|---|---|---|
| **GitHub Releases** (Direct APK) | Automated via GitHub Actions (or local build) | **You** (with your `hermit-release.jks` key) | Compatible across all GitHub Releases and in-app updates. |
| **F-Droid (Official Repo)** | F-Droid build servers from source code | **F-Droid signing server** (with F-Droid's private key) | Incompatible with GitHub APKs. Cannot cross-update without reinstalling. |
| **IzzyOnDroid (F-Droid Repo)** | Pulls pre-built binary APK from GitHub Releases | **You** (with your `hermit-release.jks` key) | Fully compatible with GitHub Releases. |

- If you submit Hermit to the main F-Droid repository in the future, F-Droid builds the APK from your source code in an isolated container and signs it with the F-Droid master key.
- Users who install Hermit from F-Droid will receive updates through the F-Droid client.
- Users who install Hermit directly from GitHub Releases or Hermit's website will receive verified updates through Hermit's built-in update mechanism signed with your key.
- This separation is standard in Android open-source development (e.g. Signal, NewPipe, VLC).
