package xyz.umarmohammad.hermit.updater

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.io.File
import java.security.MessageDigest

sealed class ApkVerificationResult {
    data class Success(
        val packageName: String,
        val versionName: String,
        val versionCode: Long,
        val certificateFingerprintSha256: String
    ) : ApkVerificationResult()

    data class Failure(
        val reason: Reason,
        val message: String
    ) : ApkVerificationResult() {
        enum class Reason {
            FILE_NOT_FOUND,
            CORRUPT_ARCHIVE,
            PACKAGE_NAME_MISMATCH,
            VERSION_DOWNGRADE,
            SIGNING_CERTIFICATE_MISMATCH,
            UNKNOWN
        }
    }
}

object ApkSecurityVerifier {

    /**
     * Verifies the downloaded APK before launching Android's package installer.
     * Checks package name, version, and signing certificates against the currently installed app.
     */
    fun verifyApk(context: Context, apkFile: File): ApkVerificationResult {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            return ApkVerificationResult.Failure(
                ApkVerificationResult.Failure.Reason.FILE_NOT_FOUND,
                "The downloaded APK file does not exist or is empty."
            )
        }

        val pm = context.packageManager

        // Query archive info
        val archiveInfo = getArchivePackageInfo(pm, apkFile)
            ?: return ApkVerificationResult.Failure(
                ApkVerificationResult.Failure.Reason.CORRUPT_ARCHIVE,
                "Unable to parse APK metadata. The file may be corrupt or incomplete."
            )

        // 1. Verify package ID matches Hermit
        val apkPackageName = archiveInfo.packageName
        val currentPackageName = context.packageName
        if (apkPackageName != currentPackageName) {
            return ApkVerificationResult.Failure(
                ApkVerificationResult.Failure.Reason.PACKAGE_NAME_MISMATCH,
                "Package name mismatch: The APK is for '$apkPackageName', but Hermit is '$currentPackageName'."
            )
        }

        // 2. Verify version
        val apkVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archiveInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            archiveInfo.versionCode.toLong()
        }

        val currentVersionCode = try {
            val currentInfo = pm.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                currentInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                currentInfo.versionCode.toLong()
            }
        } catch (_: Exception) {
            0L
        }

        val apkVersionName = archiveInfo.versionName ?: "unknown"
        val currentVersionName = try {
            pm.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }

        val apkSemVer = SemanticVersion.parse(apkVersionName)
        val currentSemVer = SemanticVersion.parse(currentVersionName)

        if (apkSemVer != null && currentSemVer != null) {
            if (!apkSemVer.isNewerThan(currentSemVer) && apkVersionCode < currentVersionCode) {
                return ApkVerificationResult.Failure(
                    ApkVerificationResult.Failure.Reason.VERSION_DOWNGRADE,
                    "Version downgrade detected: APK is v$apkVersionName (code $apkVersionCode), current is v$currentVersionName (code $currentVersionCode)."
                )
            }
        }

        // 3. Verify signing certificate matches the installed app
        val installedSignatures = getInstalledSignatures(pm, context.packageName)
        val apkSignatures = getArchiveSignatures(archiveInfo)

        if (installedSignatures.isNotEmpty() && apkSignatures.isNotEmpty()) {
            val matches = compareSignatures(installedSignatures, apkSignatures)
            if (!matches) {
                val installedFp = installedSignatures.firstOrNull()?.let { computeCertFingerprint(it) } ?: "unknown"
                val apkFp = apkSignatures.firstOrNull()?.let { computeCertFingerprint(it) } ?: "unknown"
                return ApkVerificationResult.Failure(
                    ApkVerificationResult.Failure.Reason.SIGNING_CERTIFICATE_MISMATCH,
                    "Signing certificate mismatch! Installed: $installedFp, Downloaded APK: $apkFp."
                )
            }
        }

        val certFingerprint = apkSignatures.firstOrNull()?.let { computeCertFingerprint(it) } ?: "verified"

        return ApkVerificationResult.Success(
            packageName = apkPackageName,
            versionName = apkVersionName,
            versionCode = apkVersionCode,
            certificateFingerprintSha256 = certFingerprint
        )
    }

    /**
     * Compares installed certificate signatures against APK signatures.
     * Pure function for easy testing and verification.
     */
    fun compareSignatures(installed: List<ByteArray>, incoming: List<ByteArray>): Boolean {
        if (installed.isEmpty() || incoming.isEmpty()) return false
        val installedFingerprints = installed.map { computeCertFingerprint(it) }.toSet()
        val incomingFingerprints = incoming.map { computeCertFingerprint(it) }.toSet()
        return installedFingerprints.intersect(incomingFingerprints).isNotEmpty()
    }

    fun computeCertFingerprint(certBytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(certBytes)
        return hash.joinToString(":") { "%02X".format(it) }
    }

    @Suppress("DEPRECATION")
    private fun getArchivePackageInfo(pm: PackageManager, apkFile: File): PackageInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.PackageInfoFlags.of(
                        (PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_ACTIVITIES).toLong()
                    )
                )
            } else {
                pm.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.GET_SIGNATURES or PackageManager.GET_ACTIVITIES
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun getInstalledSignatures(pm: PackageManager, packageName: String): List<ByteArray> {
        return try {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                PackageManager.GET_SIGNATURES
            }

            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                pm.getPackageInfo(packageName, flags)
            }

            val signingInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.signingInfo else null
            val signatures = info.signatures

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && signingInfo != null) {
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners?.map { it.toByteArray() } ?: emptyList()
                } else {
                    signingInfo.signingCertificateHistory?.map { it.toByteArray() } ?: emptyList()
                }
            } else if (signatures != null) {
                signatures.map { it.toByteArray() }
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Suppress("DEPRECATION")
    private fun getArchiveSignatures(info: PackageInfo): List<ByteArray> {
        return try {
            val signingInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.signingInfo else null
            val signatures = info.signatures

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && signingInfo != null) {
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners?.map { it.toByteArray() } ?: emptyList()
                } else {
                    signingInfo.signingCertificateHistory?.map { it.toByteArray() } ?: emptyList()
                }
            } else if (signatures != null) {
                signatures.map { it.toByteArray() }
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
