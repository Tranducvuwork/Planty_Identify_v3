import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.parcelize)
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics)
    alias(libs.plugins.dev.ksp)
}

android {
    namespace = "com.expeditee.plant.identifier"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.expeditee.planty.identifier"
        minSdk = 24
        targetSdk = 36
        versionCode = 100
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProps = Properties()
        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use {
            localProps.load(it)
        }
        val nvidiaApiKey = (localProps.getProperty("NVIDIA_API_KEY") ?: "")
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        buildConfigField("String", "NVIDIA_API_KEY", "\"$nvidiaApiKey\"")
        buildConfigField("String", "NVIDIA_API_BASE_URL", "\"https://integrate.api.nvidia.com/v1/\"")
        buildConfigField("String", "NVIDIA_MODEL", "\"nvidia/nemotron-3.5-lightning-30b-a3b\"")

        val formatter = DateTimeFormatter.ofPattern("MM.dd.yyyy")
        val formattedDate = LocalDate.now().format(formatter)
        val customFileName = "Planty_Identify_v${versionName}_v${versionCode}_${formattedDate}"
        base {
            archivesName.set(customFileName)
        }
    }

    buildTypes {
        debug {
            // Production Plant Scanner API only (no local/ngrok/planty-identify).
            val plantApiBase = (project.findProperty("plantApiBaseUrl") as String?)
                ?: "https://identifierapp-server-hmvz.onrender.com/"
            buildConfigField("String", "PLANT_API_BASE_URL", "\"$plantApiBase\"")
            buildConfigField("boolean", "USE_LOCAL_PLANT_API", "true")

            manifestPlaceholders["ad_app_id"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "planty_open_resume", "\"ca-app-pub-3940256099942544/9257395921\"")
            buildConfigField("String", "planty_inter_splash", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "planty_inter_splash_2ID", "\"ca-app-pub-3940256099942544/1033173712\"")

            buildConfigField("String", "planty_native_language", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "planty_native_language_2ID", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "planty_native_language_click", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "planty_native_language_click_2ID", "\"ca-app-pub-3940256099942544/2247696110\"")

            buildConfigField("String", "planty_native_onboarding_1", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "planty_native_onboarding_1_2ID", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "planty_native_onboarding_full", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "planty_native_onboarding_full_2ID", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "planty_native_onboarding_last", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "planty_native_onboarding_last_2ID", "\"ca-app-pub-3940256099942544/2247696110\"")

            buildConfigField("String", "planty_inter_shared", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "planty_inter_shared_2ID", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "planty_scan_reward", "\"ca-app-pub-3940256099942544/5224354917\"")

            buildConfigField("String", "planty_collapse_banner_home", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "planty_collapse_banner_shared", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "planty_native_shared", "\"ca-app-pub-3940256099942544/2247696110\"")

            buildConfigField("String", "planty_native_splash", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "planty_native_splash_2ID", "\"ca-app-pub-3940256099942544/2247696110\"")

            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            firebaseCrashlytics.mappingFileUploadEnabled = false
        }
        release {
            buildConfigField(
                "String",
                "PLANT_API_BASE_URL",
                "\"https://identifierapp-server-hmvz.onrender.com/\"",
            )
            buildConfigField("boolean", "USE_LOCAL_PLANT_API", "true")

            manifestPlaceholders["ad_app_id"] = "ca-app-pub-5199643356270953~2793354338"
            buildConfigField("String", "planty_open_resume", "\"ca-app-pub-5199643356270953/1028345518\"")
            buildConfigField("String", "planty_inter_splash", "\"ca-app-pub-5199643356270953/9621151405\"")
            buildConfigField("String", "planty_inter_splash_2ID", "\"ca-app-pub-5199643356270953/3980673846\"")

            buildConfigField("String", "planty_native_language", "\"ca-app-pub-5199643356270953/6433926886\"")
            buildConfigField("String", "planty_native_language_2ID", "\"ca-app-pub-5199643356270953/3148076545\"")
            buildConfigField("String", "planty_native_language_click", "\"ca-app-pub-5199643356270953/6638626777\"")
            buildConfigField("String", "planty_native_language_click_2ID", "\"ca-app-pub-5199643356270953/2703251194\"")

            buildConfigField("String", "planty_native_onboarding_1", "\"ca-app-pub-5199643356270953/1390169526\"")
            buildConfigField("String", "planty_native_onboarding_1_2ID", "\"ca-app-pub-5199643356270953/5125400407\"")
            buildConfigField("String", "planty_native_onboarding_full", "\"ca-app-pub-5199643356270953/9174428843\"")
            buildConfigField("String", "planty_native_onboarding_full_2ID", "\"ca-app-pub-5199643356270953/9521913200\"")
            buildConfigField("String", "planty_native_onboarding_last", "\"ca-app-pub-5199643356270953/5582668199\"")
            buildConfigField("String", "planty_native_onboarding_last_2ID", "\"ca-app-pub-5199643356270953/2499237062\"")

            buildConfigField("String", "planty_inter_shared", "\"ca-app-pub-5199643356270953/5681906399\"")
            buildConfigField("String", "planty_inter_shared_2ID", "\"ca-app-pub-5199643356270953/3789102151\"")
            buildConfigField("String", "planty_scan_reward", "\"ca-app-pub-5199643356270953/4368824728\"")

            buildConfigField("String", "planty_collapse_banner_home", "\"ca-app-pub-5199643356270953/2476020483\"")
            buildConfigField("String", "planty_collapse_banner_shared", "\"ca-app-pub-5199643356270953/6548265501\"")
            buildConfigField("String", "planty_native_shared", "\"ca-app-pub-5199643356270953/1186155393\"")

            buildConfigField("String", "planty_native_splash", "\"ca-app-pub-5199643356270953/6210566573\"")
            buildConfigField("String", "planty_native_splash_2ID", "\"ca-app-pub-5199643356270953/1325484095\"")


            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            firebaseCrashlytics.mappingFileUploadEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += setOf("META-INF/*")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }
    bundle {
        language {
            enableSplit = false
        }
    }
}

dependencies {

    implementation(project(":ads"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.koin.android)
    implementation(libs.koin.core)
    implementation(libs.gson)
    implementation(libs.glide)
    implementation(libs.glide.transform)
    implementation(libs.permissions.dispatcher)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.lottie)
    implementation(libs.dots.indicator)
    implementation(libs.rounded.image.view)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.markwon.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.threetenabp)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(project(":lamma-consent"))

    implementation(platform("com.google.firebase:firebase-bom:34.8.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-ai:17.8.0")
    implementation("com.sun.mail:android-mail:1.6.2")
    implementation("com.sun.mail:android-activation:1.6.2")
    implementation("com.chibatching.kotpref:enum-support:2.13.1")
    implementation("com.chibatching.kotpref:kotpref:2.13.1")
    implementation("com.chibatching.kotpref:gson-support:2.13.1")
    implementation("com.chibatching.kotpref:livedata-support:2.13.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.5")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation(libs.androidx.paging)

    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.cameraview)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.gravity.snap.helper)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.okhttp)
    implementation(libs.okhttp3.logging.interceptor)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("com.google.ai.edge.litert:litert:1.4.1")
    implementation("com.google.ai.edge.litert:litert-api:1.4.1")
    implementation("com.google.ai.edge.litert:litert-support:1.4.1")
    implementation("com.google.ai.edge.litert:litert-metadata:1.4.1")

    // Network & Retrofit
    implementation(libs.okhttp3)
    implementation(libs.okhttp3.logging.interceptor)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.converter.scalars)

    // Room Database
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.paging)
    ksp(libs.androidx.room.compiler)

    // Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.gms:play-services-maps:19.0.0")

    // ML Kit Translate + QR barcode
    implementation("com.google.mlkit:translate:17.0.3")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    //number picker
    implementation(libs.number.picker)
    implementation(libs.blurview)
}
