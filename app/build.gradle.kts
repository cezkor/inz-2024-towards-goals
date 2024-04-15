

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("app.cash.sqldelight") version "2.0.1"
}

android {
    namespace = "org.cezkor.towardsgoalsapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.cezkor.towardsgoalsapp"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

    }

    sourceSets["main"].res {
        srcDir("src/main/res/layouts/goals")
        srcDir("src/main/res/layouts/habits")
        srcDir("src/main/res/layouts/main")
        srcDir("src/main/res/layouts/tasks")
        srcDir("src/main/res/layouts/stats")
        srcDir("src/main/res/layouts/impints")
        srcDir("src/main/res/layouts/reminders")
        srcDir("src/main/res/layouts")
        srcDir("src/main/res")
    }


    buildTypes {

        debug {
            buildConfigField("Boolean", "SHOULD_USE_TEST_DATA", "true")
            buildConfigField("Boolean", "WITH_EXAMPLE_DATA", "true")

            kotlinOptions {
                freeCompilerArgs = listOf("-Xdebug")
            }
        }

        release {
            buildConfigField("Boolean", "SHOULD_USE_TEST_DATA", "false")
            buildConfigField("Boolean", "WITH_EXAMPLE_DATA", "false")

            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8

    }
    kotlinOptions {
        jvmTarget = "1.8"

    }
    buildToolsVersion = "33.0.1"
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }


}

sqldelight {
    databases {
        create("TGDatabase") {
            packageName.set("org.cezkor.towardsgoalsapp.database")
            srcDirs.from("src/main/sqldelight")

        }
    }

}

dependencies {

    // https://stackoverflow.com/questions/56639529/
    // duplicate-class-com-google-common-util-concurrent-listenablefuture-found-in-modu
    implementation("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")

    implementation("com.github.signaflo:timeseries:0.4")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("app.cash.sqldelight:android-driver:2.0.1")
    implementation("app.cash.sqldelight:primitive-adapters:2.0.1")
    implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.concurrent:concurrent-futures-ktx:1.1.0")
    implementation("androidx.recyclerview:recyclerview:1.3.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    testImplementation("app.cash.sqldelight:sqlite-driver:2.0.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.truth:truth:1.2.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}


