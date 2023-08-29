plugins {
    kotlin("multiplatform") version "1.9.0"
}
repositories {
    mavenCentral()
}

kotlin {
    jvm()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":"))

                dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                    // this project uses projects from the graphstream umbrella
                    // you can find their code at https://github.com/graphstream
                    implementation("org.graphstream:gs-core:2.0")
                    implementation("org.graphstream:gs-ui-swing:2.0")
                }
            }
        }
    }
}
