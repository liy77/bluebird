import de.marcphilipp.gradle.nexus.NexusPublishExtension
import io.codearte.gradle.nexus.NexusStagingExtension
import java.time.Duration

plugins {
    id("java")
    id("maven-publish")
    id("signing")
    id("io.codearte.nexus-staging") version "0.30.0"
    id("de.marcphilipp.nexus-publish") version "0.4.0"
}

group = "net.bluebird"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains:annotations:24.0.0")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly("org.jetbrains:annotations:23.0.0")
}

fun generatePom(pom: MavenPom) {
    pom.packaging = "jar"
    pom.name.set(project.name)
    pom.description.set("Javascript-Like Promise for Java")
    pom.url.set("https://github.com/JustAWaifuHunter/bluebird")
    pom.scm {
        url.set("https://github.com/JustAWaifuHunter/bluebird")
        connection.set("scm:git:git://github.com/JustAWaifuHunter/bluebird")
        developerConnection.set("scm:git:ssh:git@github.com:JustAWaifuHunter/bluebird")
    }
    pom.licenses {
        license {
            name.set("The MIT License")
            distribution.set("repo")
        }
    }
    pom.developers {
        developer {
            id.set("Liy")
            name.set("Liy")
            email.set("liysk9@protonmail.com")
        }
    }
}

val javadoc: Javadoc by tasks;

val sourcesJar = task<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from("src/main/java")
}

val javadocJar = task<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(javadoc.destinationDir)
}

publishing {
    publications {
        register("Release", MavenPublication::class) {
            groupId = "net.bluebird"
            artifactId = "async"
            version = "1.0"

            from(components["java"])
            artifact(sourcesJar)
            generatePom(pom);
        }
    }
}

fun getProjectProperty(name: String) = project.properties[name] as? String

val canSign = getProjectProperty("signing.keyId") != null;
if (canSign) {
    signing {
        sign(publishing.publications.getByName("Release"))
    }
}

configure<NexusStagingExtension> {
    username = getProjectProperty("ossrhUser") ?: ""
    password = getProjectProperty("ossrhPassword") ?: ""
}

configure<NexusPublishExtension> {
    nexusPublishing {
        repositories.sonatype {
            username.set(getProjectProperty("ossrhUser") ?: "")
            password.set(getProjectProperty("ossrhPassword") ?: "")
        }

        connectTimeout.set(Duration.ofMinutes(1))
        clientTimeout.set(Duration.ofMinutes(10))
    }
}

