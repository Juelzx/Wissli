import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

// Google-OAuth-Web-Client-ID für Google Sign-In (Credential Manager). Kommt aus der Firebase-
// Konsole (Authentication → Sign-in method → Google → Web-Client-ID) bzw. später automatisch aus
// google-services.json (R.string.default_web_client_id). Bis das Firebase-Projekt existiert, bleibt
// der Wert leer und Google Sign-In lässt sich nicht testen. NICHT ins Repo committen — Eintrag in
// der lokalen (gitignorten) local.properties: GOOGLE_WEB_CLIENT_ID=...apps.googleusercontent.com
val localProperties =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }
val googleWebClientId: String = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID", "")

android {
    namespace = "com.wissli.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.wissli.app"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Unit-Tests laufen ohne Robolectric/echtes Android-Framework. Android-SDK-Stubs würfen sonst
    // bei jedem Aufruf "not mocked" — Default-Werte reichen z. B. für einen reinen Context.Platzhalter
    // wie in LoginViewModelTest (ContextWrapper(null) als Fake-Context).
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

// Room-Schemas für spätere Migrationen versionieren, sobald Entities existieren.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Statische Analyse (Code Smells, Komplexität, potenzielle Bugs). Formatierung/Style
// übernimmt bewusst ktlint (siehe unten) — kein detekt-rules-ktlint-wrapper, um doppelte
// bzw. widersprüchliche Regeln zu vermeiden.
detekt {
    buildUponDefaultConfig = true
    allRules = false
    parallel = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

// Formatierung/Code-Style nach den offiziellen Kotlin-Konventionen, inkl. Android-Regeln.
ktlint {
    android.set(true)
    ignoreFailures.set(false)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Dependency Injection
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Lokale Persistenz (Room 3.0, siehe CLAUDE.md — expliziter SQLiteDriver ist Pflicht,
    // sqlite-bundled statt sqlite-framework für spätere KMP-Portierbarkeit)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.sqlite.bundled)
    ksp(libs.androidx.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Google Sign-In (Credential Manager)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.googleid)

    // Firebase (Versionen kommen aus der BOM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
