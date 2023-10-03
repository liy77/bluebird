plugins {
    id("java")
    id("maven-publish")
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

val sourcesJar = task<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from("src/main/java")
}
println(layout.buildDirectory.dir("repos/releases").get().toString())

publishing {
    publications {
        create<MavenPublication>("Release") {
            groupId = "io.github.justawaifuhunter"
            artifactId = "async"
            version = "1.0"

            from(components["java"])
            artifact(sourcesJar)
            generatePom(pom);
        }

        repositories {
            maven {
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = getProjectProperty("ossrhUser")
                    password = getProjectProperty("ossrhPassword")
                }
            }
        }
    }
}

fun getProjectProperty(name: String) = project.properties[name] as? String