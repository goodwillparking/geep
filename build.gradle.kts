import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.3.61"
    jacoco
    `maven-publish`
    signing
}

group = "com.github.goodwillparking"
version = "1.0.1"

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

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

val javadocJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks["javadoc"])
}

publishing.publications.create<MavenPublication>("mavenJava") {

    artifact(sourcesJar)
    artifact(javadocJar)

    pom {
        name.set(project.name)
        description.set("Geep is state machine library for Kotlin.")
        url.set("https://github.com/goodwillparking/geep")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        developers {
            developer {
                name.set("William Parker")
                email.set("willsy9919+geep@gmail.com")
            }
        }

        scm {
            connection.set("scm:git:https://github.com/goodwillparking/geep.git")
            developerConnection.set("scm:git:https://github.com/goodwillparking/geep.git")
            url.set("https://github.com/goodwillparking/geep")
        }

        from(components.findByName("java"))
    }
}

publishing {
    repositories {
        maven {
            val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
            val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)

            authentication {
                credentials {
                    username = findProperty("ossrhUsername") as String
                    password = findProperty("ossrhPassword") as String
                }
            }
        }
    }

}

signing {
    sign(publishing.publications["mavenJava"])
    sign(configurations.archives.get())
}
