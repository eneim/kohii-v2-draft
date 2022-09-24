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
  id("kotlin-kapt")
  id("kotlin-parcelize")
}

android {
  namespace = "kohii.v2.demo"

  defaultConfig {
    applicationId = "kohii.v2.demo"
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

  packagingOptions {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

dependencies {
  implementation(project(mapOf("path" to ":kohii-libs")))
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

  implementation(libs.androidx.material)
  implementation(libs.androidx.constraintlayout)

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

  implementation(libs.compose.foundation)
  implementation(libs.compose.ui)
  implementation(libs.compose.material)

  implementation(libs.airbnb.epoxy.core)
  kapt(libs.airbnb.epoxy.processor)

  implementation(libs.coil.common)

  implementation(libs.square.moshi)
  implementation(libs.square.moshi.adapters)
  kapt(libs.square.moshi.codegen)

  debugImplementation(libs.square.leakcanary)
  implementation(libs.square.leakcanary.plumber)

  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.1.3")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
