import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.security.MessageDigest
import java.io.File
import java.util.Properties
import java.util.regex.Pattern

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.kapt")
    id("androidx.baselineprofile")
}

val userHome = System.getenv("USERPROFILE")?.takeIf(String::isNotBlank)
    ?: System.getProperty("user.home")
val signingPropertiesFile = providers.environmentVariable("SCHEDULEPLUS_SIGNING_PROPERTIES")
    .orNull?.let(::file) ?: File(userHome, ".scheduleplus/signing.properties")
val localSigningProperties = Properties().apply {
    if (signingPropertiesFile.isFile) signingPropertiesFile.inputStream().use(::load)
}
fun releaseSecret(propertyName: String, environmentName: String): String? =
    providers.gradleProperty(propertyName)
        .orElse(providers.environmentVariable(environmentName))
        .orNull ?: localSigningProperties.getProperty(propertyName)

val releaseStoreFilePath = releaseSecret("sleepdown.releaseStoreFile", "SLEEPDOWN_RELEASE_STORE_FILE")
val releaseStorePassword = releaseSecret("sleepdown.releaseStorePassword", "SLEEPDOWN_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseSecret("sleepdown.releaseKeyAlias", "SLEEPDOWN_RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseSecret("sleepdown.releaseKeyPassword", "SLEEPDOWN_RELEASE_KEY_PASSWORD")
val remoteConfigSecret = releaseSecret("sleepdown.remoteConfigSecret", "SLEEPDOWN_REMOTE_CONFIG_SECRET").orEmpty()
val vivoCourseScene = providers.gradleProperty("sleepdown.vivoCourseScene").orElse("").get()
val vivoApprovedCertSha256 = providers.gradleProperty("sleepdown.vivoApprovedCertSha256").orElse("").get()
require(vivoCourseScene.isEmpty() || Regex("[A-Z][A-Z0-9_]*").matches(vivoCourseScene)) {
    "sleepdown.vivoCourseScene must be the exact approved vivo scene identifier"
}
require(vivoApprovedCertSha256.isEmpty() || Regex("[A-Fa-f0-9]{64}").matches(vivoApprovedCertSha256)) {
    "sleepdown.vivoApprovedCertSha256 must be the approved signing certificate SHA-256"
}
val schedulePlusApplicationId = "com.scheduleplus.student"
val sleepDownVersionName = providers.gradleProperty("sleepdown.versionName").orElse("1.0.1").get()
val sleepDownVersionCode = providers.gradleProperty("sleepdown.versionCode").map { it.toInt() }.getOrElse(36)
val skipReleaseResourceShrink = providers.gradleProperty("sleepdown.skipReleaseResourceShrink")
    .map(String::toBoolean)
    .getOrElse(false)
val hasReleaseSigning = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

@Suppress("UnstableApiUsage")
android {
        namespace = "com.xiaomanjun.sleepdownschedule"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }
    buildFeatures {
        buildConfig = true
    }
    lint {
        disable += setOf(
            "NullSafeMutableLiveData",
            "RememberInComposition",
            "FrequentlyChangingValue",
            "AutoboxingStateCreation",
            "ObsoleteLintCustomCheck",
            "GradleDependency",
            "VectorPath",
            "NestedWeights",
            "UnusedResources",
            "IconLauncherShape",
            "IconLocation",
            "IconDuplicates",
            // API 37 is still used only for compilation; changing target behavior is a release decision.
            "OldTargetApi",
            // The benchmark variant must stay unshrunk so baseline-profile tooling can inspect it.
            "NotShrinkingResources"
        )
    }

    defaultConfig {
        applicationId = schedulePlusApplicationId
        minSdk = 26
        targetSdk = 36
        versionCode = sleepDownVersionCode
        versionName = sleepDownVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "VIVO_COURSE_SCENE", "\"$vivoCourseScene\"")
        buildConfigField("String", "VIVO_APPROVED_CERT_SHA256", "\"${vivoApprovedCertSha256.uppercase()}\"")
        buildConfigField(
            "boolean",
            "SLEEPDOWN_LARGE_GLASS_EXPERIMENT",
            "true"
        )
    }

    sourceSets {
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(requireNotNull(releaseStoreFilePath))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        val glassOcclusionMode = providers.gradleProperty("sleepdown.glassOcclusionMode")
            .getOrElse("legacy")
        require(glassOcclusionMode in setOf("legacy", "retained", "live"))
        all {
            buildConfigField("String", "GLASS_OCCLUSION_MODE", "\"legacy\"")
            buildConfigField("boolean", "GLASS_FIXED_MORPH", "false")
        }
        getByName("debug") {
            buildConfigField("boolean", "GLASS_FIXED_MORPH", providers.gradleProperty("sleepdown.glassFixedMorph").getOrElse("false").toBoolean().toString())
            buildConfigField("String", "GLASS_OCCLUSION_MODE", "\"$glassOcclusionMode\"")
            applicationIdSuffix = ".debug"
            buildConfigField("String", "SLEEPDOWN_REMOTE_CONFIG_SECRET", "\"\"")
            buildConfigField("boolean", "SLEEPDOWN_REMOTE_AI_ENABLED", "false")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = !skipReleaseResourceShrink
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
            buildConfigField("String", "SLEEPDOWN_REMOTE_CONFIG_SECRET", "\"$remoteConfigSecret\"")
            buildConfigField("boolean", "SLEEPDOWN_REMOTE_AI_ENABLED", remoteConfigSecret.isNotBlank().toString())
        }
        create("benchmark") {
            initWith(getByName("release"))
            buildConfigField("boolean", "GLASS_FIXED_MORPH", providers.gradleProperty("sleepdown.glassFixedMorph").getOrElse("false").toBoolean().toString())
            buildConfigField("String", "GLASS_OCCLUSION_MODE", "\"$glassOcclusionMode\"")
            matchingFallbacks += listOf("release")
            applicationIdSuffix = ".benchmark"
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("github") {
            dimension = "distribution"
            buildConfigField("String", "DISTRIBUTION_CHANNEL", "\"github\"")
            buildConfigField("boolean", "SLEEPDOWN_EXPERIMENTAL_FEATURES", "true")
        }
        create("store") {
            dimension = "distribution"
            buildConfigField("String", "DISTRIBUTION_CHANNEL", "\"store\"")
            buildConfigField("boolean", "SLEEPDOWN_EXPERIMENTAL_FEATURES", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

androidComponents {
    onVariants(selector().withName(Pattern.compile("(github|store)BenchmarkRelease"))) { variant ->
        variant.applicationId.set("${variant.applicationId.get()}.benchmark")
    }
}

tasks.configureEach {
    val createsReleaseArtifact = name.matches(Regex("(assemble|bundle|package).*Release$"))
    if (createsReleaseArtifact) {
        doFirst {
            check(hasReleaseSigning) {
                "SleepDown release signing is missing. Configure sleepdown.releaseStoreFile, " +
                    "sleepdown.releaseStorePassword, sleepdown.releaseKeyAlias and " +
                    "sleepdown.releaseKeyPassword (or the matching SLEEPDOWN_RELEASE_* environment variables)."
            }
        }
    }
}

// Keep the canonical Release asset name for the updater; export a readable local delivery name.
tasks.register<Copy>("exportBrandedGithubReleaseApk") {
    group = "distribution"
    dependsOn("assembleGithubRelease")
    from(layout.buildDirectory.file("outputs/apk/github/release/app-github-release.apk"))
    into(layout.buildDirectory.dir("outputs/delivery"))
    rename { "时序清单-v$sleepDownVersionName-正式版.apk" }
}

tasks.register<Copy>("exportBrandedGithubDebugApk") {
    group = "distribution"
    dependsOn("assembleGithubDebug")
    from(layout.buildDirectory.file("outputs/apk/github/debug/app-github-debug.apk"))
    into(layout.buildDirectory.dir("outputs/delivery"))
    rename { "时序清单-v$sleepDownVersionName-预览版.apk" }
}

tasks.register("generateSchedulePlusUpdateManifest") {
    group = "release"
    description = "Build a signed GitHub Release APK and generate its SHA-256 update manifest."
    dependsOn("assembleGithubRelease")
    val releaseTag = providers.gradleProperty("sleepdown.releaseTag").orElse("v$sleepDownVersionName")
    doLast {
        val tag = releaseTag.get()
        require(tag == "v$sleepDownVersionName" && Regex("v\\d+\\.\\d+\\.\\d+(?:_beta\\d+)?").matches(tag)) {
            "sleepdown.releaseTag must match sleepdown.versionName"
        }
        val apk = layout.buildDirectory.file("outputs/apk/github/release/app-github-release.apk").get().asFile
        check(apk.isFile) { "Signed GitHub Release APK was not produced: $apk" }
        val digest = MessageDigest.getInstance("SHA-256")
        apk.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val sha256 = digest.digest().joinToString("") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
        val releaseRoot = "https://github.com/linkasz/-/releases"
        val output = layout.buildDirectory.file("outputs/release-manifest/scheduleplus-update.json").get().asFile
        output.parentFile.mkdirs()
        output.writeText(
            """{
  "applicationId": "$schedulePlusApplicationId",
  "versionName": "$sleepDownVersionName",
  "versionCode": $sleepDownVersionCode,
  "apkUrl": "$releaseRoot/download/$tag/app-github-release.apk",
  "sha256": "$sha256",
  "releaseUrl": "$releaseRoot/tag/$tag"
}
""".trimIndent() + "\n",
            Charsets.UTF_8
        )
        logger.lifecycle("Generated update manifest: ${output.absolutePath}")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

ksp {
    arg("room.schemaLocation", file("$projectDir/schemas").path)
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    val composeBom = platform("androidx.compose:compose-bom:2026.05.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.runtime:runtime-tracing")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core:1.15.0")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.webkit:webkit:1.14.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.metrics:metrics-performance:1.0.0")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("androidx.palette:palette-ktx:1.0.0")
    compileOnly("com.oplus.animation:viewseamless:1.0.0@aar")
    implementation(project(":kyant-backdrop"))
    implementation("io.github.kyant0:shapes:1.2.0")
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.3")
    implementation("top.yukonga.miuix.kmp:miuix-icons-android:0.9.3")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.3")
    add("githubImplementation", "dev.rikka.shizuku:api:13.1.5")
    add("githubImplementation", "dev.rikka.shizuku:provider:13.1.5")
    implementation("androidx.room:room-runtime:2.8.3")
    implementation("androidx.room:room-ktx:2.8.3")
    ksp("androidx.room:room-compiler:2.8.3")
    implementation("com.google.dagger:hilt-android:2.60.1")
    kapt("com.google.dagger:hilt-compiler:2.60.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.room:room-testing:2.8.3")
    baselineProfile(project(":benchmark"))
}
