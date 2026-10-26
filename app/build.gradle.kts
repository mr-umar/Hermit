import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "xyz.umarmohammad.hermit"
    compileSdk = 35

    defaultConfig {
        applicationId = "xyz.umarmohammad.hermit"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "1.2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    val localSigningProps = Properties().apply {
        val candidates = listOf(
            rootProject.file("signing.properties"),
            rootProject.file("local.signing.properties"),
            file("${System.getProperty("user.home")}/.hermit-signing/signing.properties")
        )
        for (candidate in candidates) {
            if (candidate.exists() && candidate.isFile) {
                candidate.inputStream().use { stream ->
                    load(stream)
                }
                break
            }
        }
    }

    fun findSigningProperty(primaryKey: String, secondaryKey: String? = null): String? {
        return System.getenv(primaryKey)
            ?: (localSigningProps.getProperty(primaryKey))
            ?: (project.findProperty(primaryKey) as? String)
            ?: secondaryKey?.let { System.getenv(it) }
            ?: secondaryKey?.let { localSigningProps.getProperty(it) }
            ?: secondaryKey?.let { project.findProperty(it) as? String }
    }

    signingConfigs {
        create("release") {
            val keystorePath = findSigningProperty("HERMIT_KEYSTORE_PATH", "KEYSTORE_FILE")
            val keystorePassword = findSigningProperty("HERMIT_KEYSTORE_PASSWORD", "KEYSTORE_PASSWORD")
            val keyAliasStr = findSigningProperty("HERMIT_KEY_ALIAS", "KEY_ALIAS") ?: "hermit"
            val keyPass = findSigningProperty("HERMIT_KEY_PASSWORD", "KEY_PASSWORD")

            if (!keystorePath.isNullOrBlank()) {
                val keystoreFile = file(keystorePath)
                if (keystoreFile.exists() && !keystorePassword.isNullOrBlank()) {
                    storeFile = keystoreFile
                    storePassword = keystorePassword
                    keyAlias = keyAliasStr
                    this.keyPassword = if (!keyPass.isNullOrBlank()) keyPass else keystorePassword
                } else if (gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }) {
                    if (!keystoreFile.exists()) {
                        throw GradleException("Release keystore not found at specified path: ${keystoreFile.absolutePath}")
                    }
                    if (keystorePassword.isNullOrBlank()) {
                        throw GradleException("HERMIT_KEYSTORE_PASSWORD is required when HERMIT_KEYSTORE_PATH is set.")
                    }
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null && releaseSigning.storeFile!!.exists()) {
                signingConfig = releaseSigning
            } else {
                signingConfig = null
                val isBuildingRelease = gradle.startParameter.taskNames.any {
                    it.contains("Release", ignoreCase = true)
                }
                if (isBuildingRelease) {
                    logger.lifecycle("NOTICE: Release signing is not configured (HERMIT_KEYSTORE_PATH is not set). Building unsigned release APK.")
                }
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX Core & Lifecycle
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose BOM & UI
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.3")

    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Preferences DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coroutines & Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
