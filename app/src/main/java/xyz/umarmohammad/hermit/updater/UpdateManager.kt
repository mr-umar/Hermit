package xyz.umarmohammad.hermit.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.umarmohammad.hermit.data.preference.PreferenceManager
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateError {
    object NetworkUnavailable : UpdateError()
    object RateLimitExceeded : UpdateError()
    object MalformedMetadata : UpdateError()
    object MissingApkAsset : UpdateError()
    data class ChecksumMismatch(val expected: String, val actual: String) : UpdateError()
    data class SecurityVerificationFailed(val reason: ApkVerificationResult.Failure.Reason, val details: String) : UpdateError()
    data class DownloadFailed(val message: String) : UpdateError()
}

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpToDate(val installedVersion: String, val lastChecked: Long) : UpdateState()
    data class UpdateAvailable(
        val release: GitHubRelease,
        val installedVersion: String,
        val latestVersion: String
    ) : UpdateState()
    data class Downloading(
        val release: GitHubRelease,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progress: Float
    ) : UpdateState()
    data class ReadyToInstall(
        val release: GitHubRelease,
        val apkFile: File,
        val sha256: String,
        val verification: ApkVerificationResult.Success
    ) : UpdateState()
    data class Error(val error: UpdateError, val message: String) : UpdateState()
}

class UpdateManager(
    private val context: Context,
    private val preferenceManager: PreferenceManager,
    private val releasesApiUrl: String = "https://api.github.com/repos/mr-umar/Hermit/releases"
) {
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private var downloadJob: Job? = null

    companion object {
        const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
        const val RETRY_COOLDOWN_MS = 60 * 60 * 1000L // 1 hour cooldown between failed attempts

        /**
         * Determines if an automatic update check should be performed on app open.
         * Runs if enabled and at least 24 hours have passed OR it's a new calendar day
         * (respecting retry cooldown).
         */
        fun shouldCheckForUpdates(
            autoCheckEnabled: Boolean,
            lastCheckedTimestamp: Long,
            lastAttemptTimestamp: Long = 0L,
            currentTime: Long = System.currentTimeMillis()
        ): Boolean {
            if (!autoCheckEnabled) return false
            if (lastCheckedTimestamp <= 0L) {
                return (currentTime - lastAttemptTimestamp) >= 60_000L
            }

            // Cooldown protection between rapid attempts
            if ((currentTime - lastAttemptTimestamp) < RETRY_COOLDOWN_MS && (currentTime - lastCheckedTimestamp) < CHECK_INTERVAL_MS) {
                return false
            }

            if (currentTime - lastCheckedTimestamp >= CHECK_INTERVAL_MS) {
                return true
            }

            return !isSameCalendarDay(lastCheckedTimestamp, currentTime)
        }

        fun isSameCalendarDay(time1: Long, time2: Long): Boolean {
            if (time1 <= 0L || time2 <= 0L) return false
            val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = time1 }
            val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = time2 }
            return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                    cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
        }
    }

    /**
     * Checks the public GitHub Releases API for the latest stable (non-draft, non-prerelease) release.
     * Uses SemVer numeric comparison to check if the release is newer than the installed app.
     */
    suspend fun checkForUpdates(manual: Boolean = true): Result<GitHubRelease?> = withContext(Dispatchers.IO) {
        val previousState = _updateState.value
        _updateState.value = UpdateState.Checking

        val installedVersionName = getInstalledVersionName()
        val installedSemVer = SemanticVersion.parse(installedVersionName)

        try {
            val url = URL(releasesApiUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 20000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Hermit-Android-App")
            }

            val responseCode = connection.responseCode

            if (responseCode == 403) {
                val errorMsg = "GitHub API rate limit reached. Please try again later or visit the repository directly."
                if (manual) {
                    _updateState.value = UpdateState.Error(UpdateError.RateLimitExceeded, errorMsg)
                } else {
                    _updateState.value = if (previousState is UpdateState.Error || previousState is UpdateState.Checking) UpdateState.Idle else previousState
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            if (responseCode != 200) {
                val errorMsg = "Server returned HTTP $responseCode while checking for updates."
                if (manual) {
                    _updateState.value = UpdateState.Error(UpdateError.NetworkUnavailable, errorMsg)
                } else {
                    _updateState.value = if (previousState is UpdateState.Error || previousState is UpdateState.Checking) UpdateState.Idle else previousState
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
            val allReleases = GitHubRelease.parseReleasesList(jsonString)
            val latestRelease = GitHubRelease.selectLatestStableRelease(allReleases)

            val now = System.currentTimeMillis()
            preferenceManager.setLastUpdateCheckTimestamp(now)

            if (latestRelease == null || latestRelease.parsedVersion == null) {
                _updateState.value = UpdateState.UpToDate(installedVersionName, now)
                return@withContext Result.success(null)
            }

            val cleanInstalled = installedVersionName.trim().removePrefix("v").removePrefix("V")
            val cleanTag = latestRelease.tagName.trim().removePrefix("v").removePrefix("V")

            val isNewer = if (installedSemVer != null) {
                latestRelease.parsedVersion.isNewerThan(installedSemVer)
            } else {
                cleanTag != cleanInstalled
            }

            if (isNewer) {
                _updateState.value = UpdateState.UpdateAvailable(
                    release = latestRelease,
                    installedVersion = installedVersionName,
                    latestVersion = latestRelease.parsedVersion.toString()
                )
                Result.success(latestRelease)
            } else {
                _updateState.value = UpdateState.UpToDate(installedVersionName, now)
                Result.success(null)
            }
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Unable to connect to GitHub Releases."
            if (manual) {
                _updateState.value = UpdateState.Error(UpdateError.NetworkUnavailable, errorMsg)
            } else {
                _updateState.value = if (previousState is UpdateState.Error || previousState is UpdateState.Checking) UpdateState.Idle else previousState
            }
            Result.failure(e)
        }
    }

    /**
     * Downloads the APK asset, verifies its SHA-256 against release checksums,
     * and performs package ID, version, and signing certificate verification.
     */
    fun startDownload(release: GitHubRelease, scope: CoroutineScope) {
        val apkAsset = release.apkAsset
        if (apkAsset == null) {
            _updateState.value = UpdateState.Error(
                UpdateError.MissingApkAsset,
                "No compatible APK asset found in this GitHub Release."
            )
            return
        }

        downloadJob?.cancel()
        downloadJob = scope.launch(Dispatchers.IO) {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            // Clean up old update files
            updatesDir.listFiles()?.forEach { it.delete() }

            val targetFile = File(updatesDir, apkAsset.name)
            val tempFile = File(updatesDir, "${apkAsset.name}.tmp")

            try {
                // Fetch checksum file if available
                val checksumContent = fetchChecksumAssetContent(release.checksumAsset)

                val url = URL(apkAsset.downloadUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "Hermit-Android-App")
                    instanceFollowRedirects = true
                }

                if (connection.responseCode !in 200..299) {
                    throw Exception("HTTP ${connection.responseCode} while downloading APK asset.")
                }

                val totalBytes = connection.contentLengthLong.takeIf { it > 0 } ?: apkAsset.sizeBytes
                var bytesDownloaded = 0L

                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesDownloaded += read
                            val progress = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
                            _updateState.value = UpdateState.Downloading(
                                release = release,
                                bytesDownloaded = bytesDownloaded,
                                totalBytes = totalBytes,
                                progress = progress.coerceIn(0f, 1f)
                            )
                        }
                    }
                }

                if (tempFile.exists()) {
                    tempFile.renameTo(targetFile)
                }

                // 1. Checksum verification (integrity check)
                val computedSha256 = ChecksumVerifier.computeSha256(targetFile)
                val expectedSha256 = ChecksumVerifier.findExpectedChecksum(apkAsset.name, checksumContent, release.body)

                if (expectedSha256 != null) {
                    if (!computedSha256.equals(expectedSha256, ignoreCase = true)) {
                        targetFile.delete()
                        _updateState.value = UpdateState.Error(
                            UpdateError.ChecksumMismatch(expectedSha256, computedSha256),
                            "SHA-256 integrity verification failed: expected $expectedSha256, computed $computedSha256."
                        )
                        return@launch
                    }
                }

                // 2. Pre-installation security check (package name, version, certificate match)
                when (val verification = ApkSecurityVerifier.verifyApk(context, targetFile)) {
                    is ApkVerificationResult.Failure -> {
                        targetFile.delete()
                        _updateState.value = UpdateState.Error(
                            UpdateError.SecurityVerificationFailed(verification.reason, verification.message),
                            verification.message
                        )
                    }
                    is ApkVerificationResult.Success -> {
                        _updateState.value = UpdateState.ReadyToInstall(
                            release = release,
                            apkFile = targetFile,
                            sha256 = computedSha256,
                            verification = verification
                        )
                    }
                }

            } catch (e: CancellationException) {
                tempFile.delete()
                targetFile.delete()
                val installedVersion = getInstalledVersionName()
                val latest = release.parsedVersion?.toString() ?: release.tagName
                _updateState.value = UpdateState.UpdateAvailable(release, installedVersion, latest)
            } catch (e: Exception) {
                tempFile.delete()
                targetFile.delete()
                val msg = e.localizedMessage ?: "Network error during APK download."
                _updateState.value = UpdateState.Error(UpdateError.DownloadFailed(msg), msg)
            }
        }
    }

    fun cancelDownload(release: GitHubRelease) {
        downloadJob?.cancel()
        val installedVersion = getInstalledVersionName()
        val latest = release.parsedVersion?.toString() ?: release.tagName
        _updateState.value = UpdateState.UpdateAvailable(release, installedVersion, latest)
    }

    /**
     * Checks if Hermit has permission to request package installs on Android 8.0+ (API 26).
     */
    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Launches the system settings screen for granting unknown app install permission to Hermit.
     */
    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Launches Android's official package installer for the verified APK.
     */
    fun launchPackageInstaller(apkFile: File): Boolean {
        if (!apkFile.exists()) return false

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun getInstalledVersionName(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    private suspend fun fetchChecksumAssetContent(checksumAsset: GitHubReleaseAsset?): String? = withContext(Dispatchers.IO) {
        if (checksumAsset == null) return@withContext null
        try {
            val conn = URL(checksumAsset.downloadUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 15000
            conn.setRequestProperty("User-Agent", "Hermit-Android-App")
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
