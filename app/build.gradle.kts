import java.util.Properties
import java.io.File
import java.util.jar.Manifest
import java.util.jar.Attributes
import java.util.jar.JarOutputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}
val signingFile = rootProject.file("signing.properties")
val releaseKeys = Properties().apply { if (signingFile.exists()) signingFile.inputStream().use { load(it) } }
android {
    namespace = "ru.arcwise.app"
    testBuildType = providers.gradleProperty("testBuildType").orElse("debug").get()
    compileSdk = 34
    defaultConfig {
        applicationId = "ru.arcwise.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    if (signingFile.exists()) signingConfigs.create("release") {
        storeFile = rootProject.file(releaseKeys.getProperty("storeFile"))
        storePassword = releaseKeys.getProperty("storePassword")
        keyAlias = releaseKeys.getProperty("keyAlias")
        keyPassword = releaseKeys.getProperty("keyPassword")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (signingFile.exists()) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
kapt { arguments { arg("room.schemaLocation", "$projectDir/schemas") } }

// Gradle 8.7 writes UTF-8 worker classpaths, while Windows JDK 17 reads native
// encoded argfiles. An ASCII manifest URL classpath supports Cyrillic folders.
tasks.withType<Test>().configureEach {
    doFirst {
        val classpathJar = File.createTempFile("arcwise-test-classpath-", ".jar")
        classpathJar.deleteOnExit()
        val manifest = Manifest().apply {
            mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
            mainAttributes[Attributes.Name.CLASS_PATH] = classpath.files.joinToString(" ") { it.toURI().toASCIIString() }
        }
        JarOutputStream(classpathJar.outputStream(), manifest).use { }
        classpath = files(classpathJar)
    }
}
