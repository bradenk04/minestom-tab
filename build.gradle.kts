plugins {
    `java-library`
    id("com.vanniktech.maven.publish") version "0.36.0"
}

repositories {
    mavenCentral()
}

group = "com.bradenkennedy"
version = "1.0.1"

dependencies {
    compileOnly(libs.minestom)
    testImplementation(libs.minestom.testing)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
}

mavenPublishing {
    coordinates(
        "com.bradenkennedy",
        "minestom-tab", project.version.toString(),)

    pom {
        name.set("Minestom Tab")
        description.set("Easily manage player visibility & tab visibility on minestom servers.")
        inceptionYear.set("2026")
        url.set("https://github.com/bradenk04/minestom-tab/")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("bradenk04")
                name.set("Braden Kennedy")
                url.set("https://www.bradenkennedy.com")
            }
        }
        scm {
            url.set("https://github.com/bradenk04/minestom-tab/")
            connection.set("scm:git:git://github.com/bradenk04/minestom-tab.git")
            developerConnection.set("scm:git:ssh://git@github.com/bradenk04/minestom-tab.git")
        }
    }
}

tasks.withType<GenerateModuleMetadata> {
    mustRunAfter("plainJavadocJar")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    // Disabled assertions due to internal Minestom environment incompatibilities in some versions
    enableAssertions = false
}
