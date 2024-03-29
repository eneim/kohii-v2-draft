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
  id("com.android.library")
  id("kotlin-android")
  id("kotlin-parcelize")
  id("com.vanniktech.maven.publish")
  // id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.11.1"
}

android {
  namespace = "kohii.v2"

  testOptions {
    unitTests.isIncludeAndroidResources = true
  }

  defaultConfig {
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  buildFeatures {
    // Enables Jetpack Compose for this module
    compose = true
  }

  composeOptions {
    kotlinCompilerExtensionVersion = libs.versions.jetpack.compose.compiler.get()
  }
}

dependencies {
  implementation(libs.androidx.media3.ui)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.exoplayer.ima)
  implementation(libs.androidx.media3.exoplayer.dash)
  implementation(libs.androidx.media3.exoplayer.hls)
  implementation(libs.androidx.media3.exoplayer.rtsp)

  implementation(libs.androidx.core)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.activity)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.fragment)

  implementation(libs.androidx.recyclerview)
  implementation(libs.androidx.viewpager)
  implementation(libs.androidx.viewpager2)

  implementation(libs.androidx.lifecycle.runtime)
  implementation(libs.androidx.lifecycle.viewmodel)
  implementation(libs.androidx.lifecycle.viewmodel.savedstate)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.lifecycle.livedata)
  implementation(libs.androidx.lifecycle.java8)
  implementation(libs.androidx.lifecycle.service)
  implementation(libs.androidx.lifecycle.process)

  // Tests
  testImplementation("androidx.test:core-ktx:1.4.0")
  testImplementation("androidx.test:rules:1.4.0")
  testImplementation("androidx.test.ext:junit-ktx:1.1.3")
  testImplementation("androidx.arch.core:core-testing:2.1.0")
  testImplementation("junit:junit:4.13.2")
  testImplementation("com.google.truth:truth:1.1.3")
  testImplementation("com.google.truth.extensions:truth-java8-extension:1.1.3")
  testImplementation("org.robolectric:robolectric:4.8.2")

  androidTestImplementation("androidx.test.ext:junit:1.1.3")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
