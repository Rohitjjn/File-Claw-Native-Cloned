import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    jacoco
}

// Triggering fresh build packaging and clearing build state artifacts for successful installation
// Decode debug.keystore from base64 if it does not exist to ensure consistent signing across container instances
val keystoreFile = rootProject.file("debug.keystore")
val base64File = rootProject.file("debug.keystore.base64")
if (base64File.exists() && !keystoreFile.exists()) {
    try {
        val base64Content = base64File.readText().trim().replace("\\s".toRegex(), "")
        val decoded = Base64.getDecoder().decode(base64Content)
        keystoreFile.writeBytes(decoded)
        logger.quiet("Successfully decoded debug.keystore from debug.keystore.base64")
    } catch (e: Exception) {
        logger.error("Failed to decode debug.keystore: ${e.message}", e)
    }
}

android {
    namespace = "com.example"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aistudio.filesclaw.apxqzy"
        minSdk = 26
        targetSdk = 34
        versionCode = (project.findProperty("versionCode") ?: project.findProperty("appVersionCode"))?.toString()?.toIntOrNull() ?: 3
        versionName = (project.findProperty("versionName") ?: project.findProperty("appVersionName"))?.toString() ?: "1.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // A project-level debug key is optional (decoded from debug.keystore.base64 above). When it is absent,
    // the Android Gradle Plugin's own debug keystore is used, so a fresh clone still builds.
    val hasProjectDebugKey = keystoreFile.exists()
    signingConfigs {
        if (hasProjectDebugKey) {
            register("debugSigning") {
                storeFile = keystoreFile
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    // Real release signing comes from the environment so no secret is ever committed:
    //   FILECLAW_KEYSTORE, FILECLAW_STORE_PASSWORD, FILECLAW_KEY_ALIAS, FILECLAW_KEY_PASSWORD
    // Without them the release build falls back to the debug key (fine for sideloading, not for Play).
    val releaseKeystore: String? = System.getenv("FILECLAW_KEYSTORE")
    if (releaseKeystore != null) {
        signingConfigs.register("release") {
            storeFile = file(releaseKeystore)
            storePassword = System.getenv("FILECLAW_STORE_PASSWORD")
            keyAlias = System.getenv("FILECLAW_KEY_ALIAS")
            keyPassword = System.getenv("FILECLAW_KEY_PASSWORD")
        }
    }

    buildTypes {
        getByName("debug") {
            if (hasProjectDebugKey) signingConfig = signingConfigs.getByName("debugSigning")
            enableUnitTestCoverage = true
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = when {
                releaseKeystore != null -> signingConfigs.getByName("release")
                hasProjectDebugKey -> signingConfigs.getByName("debugSigning")
                else -> signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=androidx.compose.foundation.ExperimentalFoundationApi")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
    packaging {
        resources {
            pickFirsts.add("**/META-INF/DEPENDENCIES")
            pickFirsts.add("**/META-INF/LICENSE")
            pickFirsts.add("**/META-INF/LICENSE.txt")
            pickFirsts.add("**/META-INF/NOTICE")
            pickFirsts.add("**/META-INF/NOTICE.txt")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Image loading (Coil)
    implementation(libs.coil.compose)
    implementation(libs.telephoto.zoomable.coil)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("com.github.mhiew:android-pdf-viewer:3.2.0-beta.3")
    implementation("org.jetbrains:markdown:0.7.3")
    implementation("androidx.webkit:webkit:1.12.1")

    // Archives and Excel (preview + light cell-value edit for single-sheet workbooks).
    // CSV/TSV is parsed by com.example.core.csv.
    implementation(libs.commons.compress)
    implementation(libs.xz)
    implementation(libs.fastexcel.reader)
    // Light-edit mode for XLSX: writes plain cell VALUES back (no formulas/styling),
    // same tier of support CSV already had. Same library family as the reader above.
    implementation(libs.fastexcel.writer)
    implementation(libs.zip4j)

    // Apache POI for DOCX manipulation is disabled for lightweight native alternative to speed up builds and avoid memory limit issues
    // implementation("org.apache.poi:poi-ooxml:5.2.5")
    // implementation("org.apache.poi:poi:5.2.5")
    // implementation("org.apache.xmlbeans:xmlbeans:5.1.1")
    // implementation("org.apache.commons:commons-compress:1.26.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// ---- Room schema export (needed to write real migrations; see AppDatabase) ----------------------
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// ---- Static analysis: gates only the pure-Kotlin core for now ------------------------------------
detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    source.setFrom("src/main/java/com/example/core")
    // Advisory until the first green run on a machine with network access (tracked in docs/TODO.md).
    ignoreFailures = true
}

// ---- Coverage for the core package (target: 80% lines) --------------------------------------------
jacoco {
    toolVersion = "0.8.11"
}

val coreClasses = fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
    include("com/example/core/**")
}
val coverageData = fileTree(layout.buildDirectory) {
    include("outputs/unit_test_code_coverage/**/*.exec", "jacoco/*.exec")
}

tasks.register<JacocoReport>("jacocoCoreReport") {
    group = "verification"
    description = "Coverage report for com.example.core (run after testDebugUnitTest)."
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(coreClasses)
    sourceDirectories.setFrom("src/main/java")
    executionData.setFrom(coverageData)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.register<JacocoCoverageVerification>("jacocoCoreVerify") {
    group = "verification"
    description = "Fails if line coverage of com.example.core drops below 80%."
    dependsOn("jacocoCoreReport")
    classDirectories.setFrom(coreClasses)
    sourceDirectories.setFrom("src/main/java")
    executionData.setFrom(coverageData)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
