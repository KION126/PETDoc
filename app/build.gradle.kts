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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Firebase BOM 버전을 명시
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

    // BOM 덕분에 버전 없이 추가 (동일 버전 자동 적용)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // 기타 라이브러리
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation ("org.tensorflow:tensorflow-lite:2.15.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // 캘린더 라이브러리
    implementation ("com.kizitonwose.calendar:view:2.0.3")

    // Google 로그인용 Play 서비스
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    // 파이어베이스 리얼타임 데이터베이스
    implementation("com.google.firebase:firebase-database")
    // 파이어베이스 스토리지 (이미지 저장)
    implementation("com.google.firebase:firebase-storage")
    // Glide (이미지 미리보기용)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
}

apply(plugin = "com.google.gms.google-services")
