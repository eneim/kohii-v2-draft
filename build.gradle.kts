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

/* buildscript {
  dependencies {
    classpath(kotlin("gradle-plugin", version = libs.versions.kotlin.get()))
  }
} */

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  kotlin("jvm") version libs.versions.kotlin.get() apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.mavenPublish) apply false
}

tasks.register("clean", Delete::class) {
  delete(rootProject.buildDir)
}

allprojects {
  afterEvaluate {
    if (hasProperty("android") && hasProperty("dependencies")) {
      dependencies {
        "coreLibraryDesugaring"("com.android.tools:desugar_jdk_libs:2.0.0")
      }
    }
  }

  plugins.withType<com.android.build.gradle.BasePlugin>().configureEach {
    extensions.findByType<com.android.build.gradle.BaseExtension>()?.apply {
      compileSdkVersion(33)
      defaultConfig {
        minSdk = 21
        targetSdk = 33
      }

      compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
      }

      @Suppress("UnstableApiUsage")
      buildFeatures.viewBinding = true
    }
  }

  tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()
  }

  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    kotlinOptions {
      jvmTarget = "11"
      freeCompilerArgs = freeCompilerArgs + arrayOf("-opt-in=kotlin.RequiresOptIn")
    }
  }
}

subprojects {
  // configuration required to produce unique META-INF/*.kotlin_module file names
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      if (project.hasProperty("POM_ARTIFACT_ID")) {
        moduleName = project.property("POM_ARTIFACT_ID") as String
      }
    }
  }
}
