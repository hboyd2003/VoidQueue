import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    java
    `maven-publish`
}

group = "uk.co.notnull"
version = "1.5-SNAPSHOT"

//https://github.com/gradle/gradle/issues/15383
val libs = the<LibrariesForLibs>()

repositories {
    mavenLocal()
    mavenCentral()

    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    maven {
        url = uri("https://repo.not-null.co.uk/releases/")
    }

    maven {
        url = uri("https://repo.not-null.co.uk/snapshots/")
    }

    maven {
        url = uri("https://maven.elytrium.net/repo/")
    }

    maven {
        url = uri("https://repo.clojars.org/")
    }

    maven {
        url = uri("https://repo.sayandev.org/snapshots/")
    }

    maven {
        url = uri("https://repo.hboyd.dev/snapshots/")
    }

    ivy {
        name = "Modrinth Maven Filename-Version Mismatch Workaround"
        url = uri("https://api.modrinth.com/maven/maven/modrinth")

        // This tells Gradle how to find artifacts
        patternLayout {
            artifact("[module]/[revision]/[classifier].[ext]")
        }

        metadataSources { artifact() }

        content {
            includeGroup("maven.modrinth.workaround")
        }
    }
}

dependencies {
    compileOnly(libs.velocityApi)
    compileOnly(libs.jetbrainsAnnotations)
    println(libs.limboApi.get().toString())
    compileOnly(libs.limboApi.get().toString())
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks {
    compileJava {
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
        options.encoding = "UTF-8"
    }
}
