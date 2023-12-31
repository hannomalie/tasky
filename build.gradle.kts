plugins {
    kotlin("multiplatform") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
}

allprojects {
    group = "de.hanno.tasky"
    repositories {
        mavenCentral()
    }
}


kotlin {

    jvm()

    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api("com.squareup.okio:okio:3.5.0")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0-RC")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
            }
        }

        val jvmMain by getting {
            dependencies {
                api("com.squareup.okio:okio:3.5.0")
            }
        }
    }
}

tasks.named("nativeTest") {
    doFirst {
        buildDir.resolve("test").mkdirs()
    }
}
