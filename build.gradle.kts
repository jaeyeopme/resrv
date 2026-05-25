import io.spring.gradle.dependencymanagement.DependencyManagementPlugin
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.openrewrite.gradle.RewriteExtension
import org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES

val jacocoLineCoverageMinimums =
    mapOf(
        "shared-kernel" to "0.85".toBigDecimal(),
        "platform-domain" to "0.85".toBigDecimal(),
        "platform-application" to "0.90".toBigDecimal(),
        "platform-adapter-web" to "0.90".toBigDecimal(),
        "platform-adapter-persistence" to "0.90".toBigDecimal(),
        "platform-api" to "0.90".toBigDecimal(),
        "timeslot-domain" to "0.85".toBigDecimal(),
        "timeslot-application" to "0.90".toBigDecimal(),
        "timeslot-adapter-web" to "0.90".toBigDecimal(),
        "timeslot-adapter-persistence" to "0.90".toBigDecimal(),
        "timeslot-booking-api" to "0.90".toBigDecimal(),
    )

plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.dependency.management) apply false
    alias(libs.plugins.openrewrite) apply false
    alias(libs.plugins.spotless)
    checkstyle
}

repositories {
    mavenCentral()
}

spotless {
    java {
        target("*/src/**/*.java")
        googleJavaFormat(libs.run { versions.google.java.format.get() }).aosp()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

subprojects {
    group = "io.resrv"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    plugins.withType<CheckstylePlugin> {
        configure<CheckstyleExtension> {
            toolVersion = "10.21.4"
            configFile = rootProject.file("config/checkstyle/checkstyle.xml")
            isIgnoreFailures = false
            maxWarnings = 0
        }
    }

    plugins.withType<DependencyManagementPlugin> {
        configure<DependencyManagementExtension> {
            imports {
                mavenBom(BOM_COORDINATES)
            }
        }
    }

    plugins.withType<JavaPlugin> {
        apply(plugin = "org.openrewrite.rewrite")

        configure<RewriteExtension> {
            setConfigFile(rootProject.file("rewrite.yml"))
            activeRecipe("io.resrv.OpenRewriteCleanup")
        }

        configure<JavaPluginExtension> {
            toolchain {
                languageVersion = JavaLanguageVersion.of(25)
            }
        }

        tasks.withType<JavaCompile> {
            options.compilerArgs.add("-parameters")
        }

        tasks.withType<Test> {
            useJUnitPlatform()
        }

        dependencies {
            add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        }

    }

    plugins.withType<JacocoPlugin> {
        tasks.withType<Test> {
            finalizedBy(tasks.named("jacocoTestReport"))
        }

        tasks.withType<JacocoReport> {
            reports {
                html.required = true
            }
        }

        tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
            violationRules {
                rule {
                    limit {
                        counter = "LINE"
                        value = "COVEREDRATIO"
                        minimum =
                            jacocoLineCoverageMinimums.getValue(project.name)
                    }
                }
            }
            dependsOn(tasks.named("test"))
        }

        tasks.named("check") {
            dependsOn(tasks.named("jacocoTestCoverageVerification"))
        }
    }
}
