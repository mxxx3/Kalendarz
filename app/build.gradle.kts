import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localFirebase = Properties().apply {
    val configuration = rootProject.file("local.properties")
    if (configuration.exists()) configuration.inputStream().use { load(it) }
}
// Public Firebase Android client configuration; environment and local overrides take precedence.
val projectFirebase = Properties().apply {
    val configuration = rootProject.file("firebase-project.properties")
    if (configuration.exists()) configuration.inputStream().use { load(it) }
}
fun firebaseSetting(name: String): String =
    listOf(System.getenv(name), localFirebase.getProperty(name), projectFirebase.getProperty(name))
        .firstOrNull { !it.isNullOrBlank() }?.trim().orEmpty()
fun gradleString(value: String): String = "\""+value.replace("\\","\\\\").replace("\"","\\\"")+"\""


android {
    namespace = "pl.mojeroboty.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "pl.mojeroboty.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.1.0"
        buildConfigField("String", "FIREBASE_API_KEY", gradleString(firebaseSetting("FIREBASE_API_KEY")))
        buildConfigField("String", "FIREBASE_APP_ID", gradleString(firebaseSetting("FIREBASE_APP_ID")))
        buildConfigField("String", "FIREBASE_PROJECT_ID", gradleString(firebaseSetting("FIREBASE_PROJECT_ID")))
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation(platform("com.google.firebase:firebase-bom:35.0.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
