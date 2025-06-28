plugins {

    id("com.android.application")

    id("org.jetbrains.kotlin.android")

    id("com.google.gms.google-services")

}



android {

    namespace = "com.ms.myworld"

    compileSdk = 34



    defaultConfig {

        applicationId = "com.ms.myworld"

        minSdk = 24

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

        sourceCompatibility = JavaVersion.VERSION_17

        targetCompatibility = JavaVersion.VERSION_17

    }

    kotlinOptions {

        jvmTarget = "17"

    }

    buildFeatures {

        viewBinding = true

    }

    packaging {

        resources.excludes.add("META-INF/DEPENDENCIES")

    }

}



dependencies {

    implementation("androidx.core:core-ktx:1.12.0")

    implementation("androidx.appcompat:appcompat:1.6.1")

    implementation("com.google.android.material:material:1.11.0")

    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")



// Firebase

    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))

    implementation("com.google.firebase:firebase-auth")

// ADD THIS LINE for Firestore

    implementation("com.google.firebase:firebase-firestore-ktx")





// Google Sign-In & Drive API Libraries

    implementation("com.google.android.gms:play-services-auth:21.2.0")

    implementation("com.google.api-client:google-api-client-android:2.0.0")

    implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0") {

        exclude(group = "org.apache.httpcomponents")

    }



// Coroutines for background tasks

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")



// UI Libraries

    implementation("com.airbnb.android:lottie:6.4.1")

    implementation("io.coil-kt:coil:2.6.0")



// Testing

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")

    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

}