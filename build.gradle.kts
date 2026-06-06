plugins {
    java
    `java-library`
    id("com.gradleup.shadow") version "9.2.2"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

group = "me.mapacheee.template"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnlyApi("me.mapacheee:MapacheeeLib:1.0.0")
    annotationProcessor("me.mapacheee:MapacheeeLib:1.0.0")

    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

tasks {
    processResources {
        filesMatching("paper-plugin.yml") {
            expand("version" to version)
        }
    }
    shadowJar {
        archiveFileName.set("PaperWinterTemplate-${project.version}.jar")
    }
}