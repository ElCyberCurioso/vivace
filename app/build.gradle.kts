import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Firma de release: si existe keystore.properties (fuera de git) se usa esa
// clave; si no, se firma con la de debug para no romper compilaciones locales.
// IMPORTANTE: para distribuir actualizaciones hay que firmar SIEMPRE con la
// misma clave de release (si cambia, las actualizaciones no se instalan encima).
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) load(FileInputStream(keystorePropsFile))
}

android {
    namespace = "com.guitarchords.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.guitarchords.app"
        minSdk = 26
        targetSdk = 35
        /*
         * versionCode es lo ÚNICO que mira la app para saber si hay versión
         * nueva (UpdateManager.check lo compara con BuildConfig.VERSION_CODE),
         * así que sube en cada publicación aunque el nombre no cambie.
         */
        versionCode = 2
        versionName = "2.0"
        vectorDrawables { useSupportLibrary = true }
        // URL base del Worker para la auto-actualización. Vacío = se usa la URL
        // de sincronización configurada en la app. Con el dominio propio ya no
        // hace falta configurar nada: las actualizaciones funcionan de salida.
        buildConfigField("String", "UPDATE_BASE_URL", "\"https://accordio.site\"")
    }

    signingConfigs {
        create("release") {
            if (keystorePropsFile.exists()) {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (keystorePropsFile.exists())
                signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

// Room escribe el esquema de cada versión en app/schemas/. Se sube al
// repositorio a propósito: es la única referencia contra la que comprobar que
// una migración deja la base igual que una instalación nueva. Sin esto, las
// 16 migraciones acumuladas no tenían nada con lo que contrastarse.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    // Sincronización en segundo plano: WorkManager guarda la petición en disco,
    // así que sobrevive a que se cierre la app y al reinicio del móvil. Un
    // NetworkCallback en el Application, que es lo que había, no.
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("sh.calvin.reorderable:reorderable:2.4.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime-livedata")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
