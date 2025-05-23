plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
}

android {
    namespace = "com.example.uberriderremake"
    compileSdk = 35
    buildFeatures {
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.example.uberriderremake"
        minSdk = 26
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Material
    implementation ("com.google.android.material:material:1.11.0") // Latest stable

// RxJava
    implementation ("io.reactivex.rxjava2:rxandroid:2.1.1") // Latest version (no updates since)
    implementation ("io.reactivex.rxjava2:rxjava:2.2.21")  // Latest stable in RxJava2

    implementation ("com.firebaseui:firebase-ui-auth:8.0.2")

    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.firebase:firebase-auth:23.2.0")

    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))

    implementation("com.google.android.play:integrity:1.4.0")

    implementation ("com.google.firebase:firebase-database-ktx:20.3.0")

    implementation ("com.google.firebase:firebase-storage-ktx:20.3.0")

    implementation ("com.github.bumptech.glide:glide:4.16.0")

    implementation ("com.google.android.gms:play-services-location:21.0.1")
    implementation ("com.karumi:dexter:6.2.3")

    implementation ("com.firebase:geofire-android:3.2.0")

    implementation ("de.hdodenhof:circleimageview:3.1.0")




}