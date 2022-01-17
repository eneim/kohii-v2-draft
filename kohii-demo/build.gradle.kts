/*
 * Copyright (c) 2021. Nam Nguyen, nam@ene.im
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
  id("com.android.application")
  id("kotlin-android")
}

android {
  compileSdk = 31

  defaultConfig {
    applicationId = "kohii.v2.demo"
    minSdk = 21
    targetSdk = 31
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    getByName("debug") {
      keyAlias = "debug"
      keyPassword = "android"
      storeFile = file("debug.jks")
      storePassword = "android"
    }
    create("release") {
      keyAlias = project.property("kohii_prodKeyAlias") as String
      keyPassword = project.property("prodKeyPassword") as String
      storeFile = file(project.property("prodStoreFile") as String)
      storePassword = project.property("prodStorePassword") as String
    }
  }

  buildTypes {
    release {
      signingConfig = signingConfigs.getByName("release")
      isShrinkResources = true
      isMinifyEnabled = true
      isDebuggable = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }

    debug {
      signingConfig = signingConfigs.getByName("debug")
      applicationIdSuffix = ".dev"
      isShrinkResources = false
      isMinifyEnabled = false
      isDebuggable = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  kotlinOptions {
    jvmTarget = "1.8"
    @Suppress("SuspiciousCollectionReassignment")
    freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
  }

  buildFeatures {
    viewBinding = true
    // Enables Jetpack Compose for this module
    compose = true
  }

  composeOptions {
    kotlinCompilerExtensionVersion = libs.versions.jetpack.compose.compiler.get()
  }
}

dependencies {
  implementation(project(mapOf("path" to ":kohii-libs")))

  implementation(libs.androidx.core)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.activity)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.fragment)

  implementation(libs.androidx.material)
  implementation(libs.androidx.constraintlayout)

  implementation(libs.androidx.recyclerview)
  implementation(libs.androidx.viewpager)
  implementation(libs.androidx.viewpager2)

  implementation(libs.airbnb.epoxy.core)

  implementation(libs.androidx.lifecycle.runtime)
  implementation(libs.androidx.lifecycle.viewmodel)
  implementation(libs.androidx.lifecycle.viewmodel.savedstate)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.lifecycle.livedata)
  implementation(libs.androidx.lifecycle.java8)
  implementation(libs.androidx.lifecycle.service)
  implementation(libs.androidx.lifecycle.process)

  // val media3Version = "1.0.0-alpha01"
  // implementation("androidx.media3:media3-common:$media3Version")
  // implementation("androidx.media3:media3-ui:$media3Version")
  // implementation("androidx.media3:media3-exoplayer:$media3Version")
  // implementation("androidx.media3:media3-exoplayer-dash:$media3Version")
  // implementation("androidx.media3:media3-exoplayer-hls:$media3Version")
  // implementation("androidx.media3:media3-exoplayer-rtsp:$media3Version")
  // implementation("androidx.media3:media3-exoplayer-smoothstreaming:$media3Version")
  // implementation("androidx.media3:media3-exoplayer-ima:$media3Version")
  // implementation("androidx.media3:media3-datasource-cronet:$media3Version")
  // implementation("androidx.media3:media3-datasource-okhttp:$media3Version")
  // implementation("androidx.media3:media3-datasource-rtmp:$media3Version")
  // implementation("androidx.media3:media3-ui-leanback:$media3Version")
  // implementation("androidx.media3:media3-session:$media3Version")
  // implementation("androidx.media3:media3-extractor:$media3Version")
  // implementation("androidx.media3:media3-cast:$media3Version")
  // implementation("androidx.media3:media3-exoplayer-workmanager:$media3Version")
  // implementation("androidx.media3:media3-transformer:$media3Version")
  // implementation("androidx.media3:media3-database:$media3Version")
  // implementation("androidx.media3:media3-decoder:$media3Version")
  // implementation("androidx.media3:media3-datasource:$media3Version")

  implementation(libs.android.exoplayer)

  // Jetpack Compose
  implementation(libs.compose.foundation)
  implementation(libs.compose.ui)
  implementation(libs.compose.material)
  implementation(libs.compose.ui.tooling)

  val composeVersion = "1.1.0-rc01"
  androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")

  implementation("io.coil-kt:coil:1.4.0")
  implementation("io.coil-kt:coil-compose:1.4.0")
  implementation("com.squareup.moshi:moshi:1.13.0")

  debugImplementation("com.squareup.leakcanary:leakcanary-android:2.8.1")
  implementation("com.squareup.leakcanary:plumber-android:2.8.1")

  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.1.3")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
