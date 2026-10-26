pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://www.jetbrains.com/intellij-repository/releases")
    }
}

plugins {
    // Downloads the Java toolchain the build asks for, so a clone needs no JDK
    // installed beyond the one running Gradle.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("org.jetbrains.intellij.platform.settings") version "2.19.0"
    id("com.gradle.develocity") version "4.6.1"
}

rootProject.name = "Testin"

// The content module that carries everything needing the IntelliJ Java plugin,
// so the Plugin Verifier stops reporting it against IDEs that have no Java
// support (#144).
include(":testin-java")
include(":testin-testng")
include(":testin-apimodel")
include(":testin-robot")

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"
    }
}