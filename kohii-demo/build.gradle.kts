plugins {
  id("com.android.application")
  id("kotlin-android")
}

android {
  compileSdk = 30
  buildToolsVersion = "30.0.3"

  defaultConfig {
    applicationId = "kohii.v2.demo"
    minSdk = 21
    targetSdk = 30
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
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

  buildFeatures {
    viewBinding = true
  }
}

dependencies {
  implementation(project(mapOf("path" to ":kohii-draft")))

  implementation("androidx.core:core-ktx:1.6.0")
  implementation("androidx.appcompat:appcompat:1.3.1")
  implementation("com.google.android.material:material:1.4.0")
  implementation("androidx.constraintlayout:constraintlayout:2.0.4")

  val lifecycleVersion = "2.3.1"
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
  implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
  implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifecycleVersion")
  implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion")
  implementation("androidx.lifecycle:lifecycle-service:$lifecycleVersion")
  implementation("androidx.lifecycle:lifecycle-process:$lifecycleVersion")

  // implementation("com.google.android.exoplayer:exoplayer:2.14.2")
  // implementation("io.coil-kt:coil:1.2.2")

  debugImplementation("com.squareup.leakcanary:leakcanary-android:2.7")

  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.1.3")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
