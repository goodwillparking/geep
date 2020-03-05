import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.3.61"
    jacoco
}

group = "io.github.goodwillparking"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation(group = "org.slf4j", name = "slf4j-api", version = "1.7.25")

    testImplementation(group = "ch.qos.logback", name = "logback-classic", version = "0.9.26")
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter", version = "5.5.2")
    testImplementation(group = "org.hamcrest", name = "hamcrest-all", version = "1.3")
    testImplementation(group = "org.mockito", name = "mockito-all", version = "1.9.5")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.named<Task>("test") {
    finalizedBy("jacocoTestReport")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
