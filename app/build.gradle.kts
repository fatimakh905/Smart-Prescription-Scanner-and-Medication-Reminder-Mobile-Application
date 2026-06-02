import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.mediremind"
    compileSdk = 36

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.mediremind"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources=true
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
val props = Properties()
val localPropertiesFile = rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { props.load(it) }
}

androidComponents {
    onVariants { variant ->
        variant.buildConfigFields?.put(
            "PADDLE_TOKEN",
            com.android.build.api.variant.BuildConfigField(
                "String",
                "\"${props.getProperty("PADDLE_TOKEN", "")}\"",
                "Paddle token"
            )
        )

        variant.buildConfigFields?.put(
            "HF_TOKEN",
            com.android.build.api.variant.BuildConfigField(
                "String",
                "\"${props.getProperty("HF_TOKEN", "")}\"",
                "HF token"
            )
        )
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // RecyclerView and CardView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // Image loading (prescription preview)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}


