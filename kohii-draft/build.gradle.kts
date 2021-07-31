plugins {
  id("com.android.library")
  id("kotlin-android")
  id("kotlin-parcelize")
}

android {
  compileSdk = 30
  buildToolsVersion = "30.0.3"

  defaultConfig {
    minSdk = 21
    targetSdk = 30

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
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
}

dependencies {
  implementation("androidx.core:core-ktx:1.6.0")
  implementation("androidx.appcompat:appcompat:1.3.1")
  implementation("com.google.android.material:material:1.4.0")

  implementation("androidx.viewpager2:viewpager2:1.0.0")
  implementation("androidx.recyclerview:recyclerview:1.2.1")
  implementation("androidx.fragment:fragment-ktx:1.3.6")
  implementation("androidx.activity:activity-ktx:1.3.0")
  implementation("androidx.media2:media2-widget:1.2.0-beta01")
  implementation("com.google.android.exoplayer:exoplayer:2.14.2")

  val lifecycleVersion = "2.3.1"
  val archVersion = "2.1.0"
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
  implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
  implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifecycleVersion")
  implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion")
  implementation("androidx.lifecycle:lifecycle-service:$lifecycleVersion")
  implementation("androidx.lifecycle:lifecycle-process:$lifecycleVersion")

  // optional - Test helpers for LiveData
  testImplementation("androidx.arch.core:core-testing:$archVersion")
  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.1.3")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
