plugins {
    kotlin("multiplatform") version "1.9.0"
}

kotlin {
    jvm()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":"))
            }
        }
    }
}
