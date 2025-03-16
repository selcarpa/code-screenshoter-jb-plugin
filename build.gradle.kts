import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.10"
    id("org.jetbrains.intellij.platform") version "2.3.0"
    idea
}

group = "one.tain.jbp.code-screenshoter"
version = "1.8.1"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies{
    implementation("org.apache.servicemix.bundles:org.apache.servicemix.bundles.batik:1.16_1")
    intellijPlatform {
        intellijIdeaCommunity("2024.1")
    }
}
tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }

    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("243.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

idea {
    module {
        isDownloadJavadoc = false
        isDownloadSources = true
    }
}
