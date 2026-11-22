plugins {
    alias(libs.plugins.feedflow.library)
}

kotlin {
    android {
        namespace = "com.prof18.feedflow.feedsync.folder"
        withHostTest {}
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":core"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.touchlab.kermit)
                implementation(libs.multiplatform.settings)
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        jvmTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.multiplatform.settings.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(project(":feedSync:dropbox"))
                implementation(project(":feedSync:googledrive"))
            }
        }

        getByName("androidHostTest") {
            dependsOn(commonTest.get())

            dependencies {
                implementation(libs.org.robolectric)
                implementation(libs.androidx.test.core.ktx)
                implementation(libs.multiplatform.settings.test)
            }
        }
    }
}
