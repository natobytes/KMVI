plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":kmvi"))
            implementation(libs.androidx.lifecycle.viewmodel.compose)
        }
    }
}
