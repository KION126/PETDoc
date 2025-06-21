plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.petdoc"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.petdoc"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8 // Java 8로 변경
        targetCompatibility = JavaVersion.VERSION_1_8 // Java 8로 변경
    }
    // Kotlin 사용 시 (필요한 경우)
    // kotlinOptions {
    //     jvmTarget = "1.8"
    // }
}

dependencies {
    // Firebase BOM 버전을 명시
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

    // BOM 덕분에 버전 없이 추가 (동일 버전 자동 적용)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-database") // 파이어베이스 리얼타임 데이터베이스
    implementation("com.google.firebase:firebase-storage") // 파이어베이스 스토리지 (이미지 저장)


    // 기타 라이브러리
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // TensorFlow Lite 의존성
    // 기본 CPU 추론 라이브러리
    implementation ("org.tensorflow:tensorflow-lite:2.15.0")


    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Glide (이미지 로딩 라이브러리)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Google 로그인용 Play 서비스
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    // Google Play 서비스의 Location API 라이브러리
    implementation("com.google.android.gms:play-services-location:21.0.1")
    // OkHttp 라이브러리
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // 네이버 지도 SDK
    implementation("com.naver.maps:map-sdk:3.21.0")
}

apply(plugin = "com.google.gms.google-services")