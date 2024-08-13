import java.io.DataInputStream
import java.net.URI
import java.util.Properties

//import org.jetbrains.kotlin.fir.declarations.builder.buildImport

plugins {
    id("java-library")
    id("maven-publish")
    alias(libs.plugins.jetbrains.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

buildscript {
    dependencies {
        classpath(libs.proguard.gradle)
    }
}

val localProperties by lazy {
    val localProperties = Properties()
    project.rootProject.file("local.properties")
        .inputStream().use {
            DataInputStream(it).use { inputStream ->
                localProperties.load(inputStream)
            }
        }
    localProperties
}
val isSnapShot = localProperties.getProperty("SNAPSHOT", "false").toBoolean()
fun getRepositoryUrl(): URI {
    return URI.create(localProperties["REPOSITORY_URL"] as? String ?: "")
}

fun getRepositoryUserName():String {
    return localProperties["REPOSITORY_USER_NAME"] as? String ?: ""
}

fun getRepositoryPassword():String {
    return localProperties["REPOSITORY_PASSWORD"] as? String ?: ""
}

val  pbGroupId = "com.cliff.reflection"
val  pbArtifactId = "reflection-common"
val  pbVersion = "1.0.0" + if (isSnapShot) "-SNAPSHOT" else ""

afterEvaluate {
    publishing {
        publications {
            register("release",MavenPublication::class) {
                from(components.named("java").get())
                artifacts {
                    groupId = pbGroupId
                    artifactId = pbArtifactId
                    version = pbVersion
                }
            }
        }

        repositories {
            maven {
                url = getRepositoryUrl()
                credentials {
                    username = getRepositoryUserName()
                    password = getRepositoryPassword()
                }
            }
        }
    }
}

