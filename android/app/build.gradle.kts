plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "ai.routin.mytoken"
    compileSdk = 34

    defaultConfig {
        applicationId = "ai.routin.mytoken"
        minSdk = 29
        targetSdk = 34
        versionCode = 621
        versionName = "6.2.1"

        // 只保留真机使用的 ARM 架构；x86/x86_64 仅模拟器需要，不再进入安装包。
        // 注意：Apple Silicon 模拟器是 arm64，不受影响；Intel Mac 的 x86 模拟器需临时移除该过滤。
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        debug {
            // 与正式版共存：debug 使用独立包名，可以在同一台设备上同时安装对比。
            applicationIdSuffix = ".debug"
            // 桌面名称与组件选择器加 Debug 后缀，便于区分两个版本。
            resValue("string", "app_name", "MyToken Debug")
            resValue("string", "widget_credential_label", "MyToken 凭证（Debug）")
            resValue("string", "widget_overview_label", "MyToken 总览（Debug）")
        }
        release {
            // R8 已开启：发布前需在真机回归添加/刷新凭证、分享、扫码导入、Widget 与通知。
            // 崩溃还原依赖 app/build/outputs/mapping/release/mapping.txt，发版时需留存。
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        jniLibs {
            // 压缩原生库以缩小下载包；安装时解压到应用目录。
            useLegacyPackaging = true
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":core-ui"))
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(project(":provider"))
    implementation(project(":feature-home"))
    implementation(project(":feature-credentials"))
    implementation(project(":feature-settings"))
    implementation(project(":feature-transfer"))

    // AppGraph 构造 Room 数据库时直接引用 androidx.room.Room，app 需要该依赖。
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.glance:glance-appwidget:1.1.0")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    androidTestImplementation(project(":feature-transfer"))
    androidTestImplementation(project(":core"))
    androidTestImplementation(project(":domain"))
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

tasks.withType<Test>().configureEach {
    systemProperty(
        "widget.contracts.dir",
        rootProject.file("../shared/widget-contracts").absolutePath,
    )
}
