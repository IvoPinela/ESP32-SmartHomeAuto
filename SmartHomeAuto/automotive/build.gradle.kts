plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.example.smarthomeauto"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.smarthomeauto"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.room.common)
    implementation(libs.androidx.room.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation ("org.eclipse.paho:org.eclipse.paho.android.service:1.1.1")
    implementation ("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.0")
    implementation ("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    annotationProcessor(libs.androidx.room.compiler)
    implementation ("org.mindrot:jbcrypt:0.4")
    implementation ("com.google.android.material:material:1.12.0")


}