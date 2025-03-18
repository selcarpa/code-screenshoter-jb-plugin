fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.gradleIntelliJPlugin) // Gradle IntelliJ Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

// Configure project's dependencies
repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    implementation("org.apache.xmlgraphics:batik-dom:1.17")
    implementation("org.apache.xmlgraphics:batik-svggen:1.17")
    intellijPlatform {
        create(properties("platformType"), properties("platformVersion"))
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })
    }
}

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(17)
}

intellijPlatform {
    pluginConfiguration {
        name = properties("pluginName")
        ideaVersion {
            sinceBuild = properties("pluginSinceBuild").get()
            untilBuild = properties("pluginUntilBuild").get()
        }
    }
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    signPlugin {
        certificateChain = environment("CERTIFICATE_CHAIN")
        privateKey = environment("PRIVATE_KEY")
        password = environment("PRIVATE_KEY_PASSWORD")
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token = environment("PUBLISH_TOKEN")
        channels = properties("pluginVersion").map {
            listOf(
                it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" })
        }
    }
}
