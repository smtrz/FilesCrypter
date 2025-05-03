plugins {
    id("com.android.library")// Android Library Plugin
    id("org.jetbrains.kotlin.android") version "2.1.10" // Kotlin Android Plugin
    id("com.google.devtools.ksp") version "2.1.10-1.0.31" // KSP Plugin
    id("maven-publish")
    kotlin("kapt")
}

android {
    namespace = "com.tahir.fileencrypter"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    ksp {
        arg("KOIN_CONFIG_CHECK", "true")
        arg("KOIN_DEFAULT_MODULE", "false")
    }

    sourceSets.getByName("main") {
        java.srcDirs("${layout.buildDirectory}/generated/ksp/main/kotlin")
    }
}

dependencies {
    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Kotlin Coroutines
    implementation(libs.coroutines.android)
    implementation(libs.kotlin.sdlib)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.core.testing)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)

    // Logging & DI
    implementation(libs.timber)
    implementation(libs.koin.android)
    implementation(libs.koin.annotations)
    ksp(libs.koin.ksp)
    testImplementation(libs.koin.test)
}
// ===== Maven Publishing Setup for local build =====
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.tahir"
                artifactId = "fileencrypter"
                version = "1.0.0"
            }
        }
    }
}