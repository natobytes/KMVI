import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import java.net.URI

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    id("maven-publish")
    id("com.vanniktech.maven.publish") version "0.34.0"
}

group = "io.github.natobytes"
//version = System.getenv()["RELEASE_NAME"] ?: "0.1.0"
version = "0.1.1"


kotlin {
    jvm()

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    val xcf = XCFramework()
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "kmvi"
            xcf.add(this)
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.androidx.lifecycle.viewmodel.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "io.github.natobytes.kmvi"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = URI("https://maven.pkg.github.com/natobytes/KMVI/")
            credentials {
                username = "natobytes"
                password = System.getenv()["DEPLOY_KEY"] ?: ""
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral(false)

    signAllPublications()

    coordinates(group.toString(), "kmvi", version.toString())

    pom {
        name = "KMVI"
        description = "Kotlin Multiplatform MVI - Architecture Library"
        inceptionYear = "2025"
        url = "https://github.com/natobytes/KMVI"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "NatoBytes"
                name = "NatoBytes"
                url = "https://github.com/natobytes/"
            }
        }
        scm {
            url = "https://github.com/natobytes/KMVI"
            connection = "scm:git:git://github.com/natobytes/KMVI.git"
            developerConnection = "scm:git:ssh://git@github.com/natobytes/KMVI.git"
        }
    }
}
