import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.3.20"
}

group = "wlparker"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    compile(group = "io.vavr", name = "vavr", version = "0.9.0")
    compile(group = "org.slf4j", name = "slf4j-api", version = "1.7.25")

    testCompile("junit", "junit", "4.12")
    testCompile("org.hamcrest", "hamcrest-all", "1.3")
    testCompile("ch.qos.logback", "logback-classic", "0.9.26")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
