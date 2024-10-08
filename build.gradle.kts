plugins {
    kotlin("jvm") version "2.0.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "pl.syntaxdevteam"
version = "1.0.1-DEV"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    compileOnly("dev.folia:folia-api:1.20.1-R0.1-SNAPSHOT")

    compileOnly("org.mariadb.jdbc:mariadb-java-client:3.4.1")
    compileOnly("org.postgresql:postgresql:42.7.4")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.maxmind.geoip2:geoip2:4.2.1")
    implementation("org.apache.ant:ant:1.10.15")
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}