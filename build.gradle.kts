import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "com.cordona"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

val springModulithGroupId = "org.springframework.modulith"

dependencies {
    implementation(libs.bundles.spring.boot)
    implementation(libs.bundles.spring.modulith)
    implementation(libs.kotlin.logging)
    testImplementation(libs.bundles.testing)
}

dependencyManagement {
    imports {
        mavenBom("$springModulithGroupId:spring-modulith-bom:${libs.versions.springModulith.get()}")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("failed")
        exceptionFormat = TestExceptionFormat.FULL
        showCauses = true
        showExceptions = true
        showStackTraces = true
    }
}

tasks.named<BootRun>("bootRun") {
    val envFile = file(".env/local.env")
    if (envFile.exists()) {
        envFile.readLines().forEach { line ->
            if (line.isNotBlank() && !line.startsWith("#") && "=" in line) {
                val (key, value) = line.split("=", limit = 2)
                environment(key.trim(), value.trim())
            }
        }
    }
}

tasks.register<Exec>("dockerBuild") {
    group = "docker"
    description = "Build Docker image using traditional Dockerfile"
    dependsOn("bootJar")
    commandLine("docker", "build", "-t", "claude-code-hooks:latest", ".")
}

tasks.register<Exec>("dockerUp") {
    group = "docker"
    description = "Build and start the application using Docker Compose"
    dependsOn("dockerBuild")
    commandLine("docker", "compose", "up", "-d")
}

tasks.register<Exec>("dockerDown") {
    group = "docker"
    description = "Stop and remove Docker containers"
    commandLine("docker", "compose", "down")
}