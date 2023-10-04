import java.time.Duration

plugins {
    id("java")
    id("maven-publish")
    id("io.github.gradle-nexus.publish-plugin") version "1.0.0"
    id("org.jetbrains.dokka") version "1.4.20"
    signing
}

group = "io.github.justawaifuhunter.bluebird"
version = "1.0.0"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
    }
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

val sourcesJar = task<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from("src/main/java")
}

val javadocJar = task<Jar>("javadocJar") {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Generate javadoc"
    archiveClassifier.set("javadoc")
    from(tasks.named("dokkaHtml"))
}

publishing {
    publications {
        register("Release", MavenPublication::class) {
            groupId = "io.github.justawaifuhunter"
            artifactId = "bluebird"
            version = project.version.toString()

            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
            generatePom(pom);
        }

        repositories {
            maven {
                name = "OSSRH"
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = getProjectProperty("ossrhUser")
                    password = getProjectProperty("ossrhPassword")
                }
            }
        }
    }
}

signing {
    val extension = extensions.getByName("publishing") as PublishingExtension
    sign(extension.publications.getByName("Release"))
}

nexusPublishing {
    repositories.sonatype {
        packageGroup = "io.github.justawaifuhunter"
        nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
        snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))

        username.set(getProjectProperty("ossrhUser"))
        password.set(getProjectProperty("ossrhPassword"))
    }
}

fun getProjectProperty(name: String) = project.properties[name] as? String