import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

// Las credenciales de Firebase NO se versionan (docs/02 §2): viven en local.properties,
// que esta en .gitignore. Si faltan, la app arranca en "modo dev" sin Firebase.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun secret(name: String): String = "\"${localProps.getProperty(name) ?: ""}\""

android {
    namespace = "com.driverapp.repartidor"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.driverapp.repartidor"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"http://169.58.190.93:8084/\""
        )
        // Restaurante fijo de esta app (SUMAQ). Los pedidos se filtran por este id.
        buildConfigField("String", "RESTAURANT_NAME", "\"SUMAQ\"")
        buildConfigField("int", "RESTAURANT_ID", "5")
        // Firebase: se leen de local.properties (FIREBASE_API_KEY, FIREBASE_APP_ID,
        // FIREBASE_PROJECT_ID, FIREBASE_STORAGE_BUCKET). Vacias = modo dev.
        buildConfigField("String", "FIREBASE_API_KEY", secret("FIREBASE_API_KEY"))
        buildConfigField("String", "FIREBASE_APP_ID", secret("FIREBASE_APP_ID"))
        buildConfigField("String", "FIREBASE_PROJECT_ID", secret("FIREBASE_PROJECT_ID"))
        // project_number: FCM no emite token sin él.
        buildConfigField("String", "FIREBASE_SENDER_ID", secret("FIREBASE_SENDER_ID"))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        viewBinding = false
        buildConfig = true
    }

    composeOptions {
        // Kotlin 1.9.0 -> Compose Compiler 1.5.0 (emparejamiento exacto)
        kotlinCompilerExtensionVersion = "1.5.0"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.fragment:fragment-ktx:1.7.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // --- Sprint 5: Jetpack Compose (BOM 2023.08 -> Compose 1.5.x) ---
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // --- Sprint 6: persistencia local (DataStore + Room) ---
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // --- S09: cola offline de transiciones ---
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    // Sólo FCM: la autenticación la hace el backend, no Firebase.
    implementation("com.google.firebase:firebase-messaging")

    implementation("org.osmdroid:osmdroid-android:6.1.20")

    implementation("androidx.security:security-crypto:1.0.0")
    implementation("com.google.code.gson:gson:2.11.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.test:core-ktx:1.5.0")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
}
