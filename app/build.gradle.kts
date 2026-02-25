plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.safesphere"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.safesphere"
        minSdk = 24
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    // 🔥 Google Maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // 📍 Location (Current user location)
    implementation("com.google.android.gms:play-services-location:21.2.0")

    implementation("androidx.fragment:fragment-ktx:1.7.1")


    // Scalable DP
    implementation("com.intuit.sdp:sdp-android:1.1.0")
    // Scalable SP (Text Size)
    implementation("com.intuit.ssp:ssp-android:1.1.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.google.android.material:material:1.12.0")

    implementation("com.airbnb.android:lottie:6.3.0")
    //cloudinary
    implementation("com.cloudinary:cloudinary-android:2.3.1")
    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.1.0"))
    // TODO: Add the dependencies for Firebase products you want to use
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth:23.0.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    //Firebase Messaging Service
     implementation("com.google.firebase:firebase-messaging:24.0.0")
    //to load photoUrl from authentication and setup in imageview
    implementation("com.squareup.picasso:picasso:2.8")
    //facebook auth
    implementation("com.facebook.android:facebook-android-sdk:latest.release")

    // Realtime Database
    implementation("com.google.firebase:firebase-database:20.3.1")

    // ✅ WorkManager (MANDATORY)
    implementation("androidx.work:work-runtime:2.9.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // --- Room Database Dependencies ---
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    // Note: Agar aap Kotlin use kar rahe hain to 'kapt' use hota hai,
    // lekin aapka code Java mein hai isliye 'annotationProcessor' sahi hai.
    annotationProcessor("androidx.room:room-compiler:$roomVersion")

    // WorkManager (Aapke paas pehle se hai, but version sync ke liye re-check kar lein)
    implementation("androidx.work:work-runtime:2.9.0")

    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-video:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

}
android {
    defaultConfig {
        vectorDrawables.useSupportLibrary = true
    }
}
