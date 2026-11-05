buildscript {
    dependencies {
        // O AGP 9 embute o plugin do Kotlin; esta entrada só fixa a versão dele.
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
