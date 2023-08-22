plugins {
    kotlin("multiplatform") version "1.9.0"
}
repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm()

    val osName = System.getProperty("os.name")
    val targetOs = when {
        osName == "Mac OS X" -> "macos"
        osName.startsWith("Win") -> "windows"
        osName.startsWith("Linux") -> "linux"
        else -> error("Unsupported OS: $osName")
    }

    val osArch = System.getProperty("os.arch")
    var targetArch = when (osArch) {
        "x86_64", "amd64" -> "x64"
        "aarch64" -> "arm64"
        else -> error("Unsupported arch: $osArch")
    }

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":"))

                val target = "${targetOs}-${targetArch}"
                dependencies {
                    implementation("org.jetbrains.skiko:skiko-awt-runtime-$target:0.7.77")
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                }
            }
        }
    }
}
