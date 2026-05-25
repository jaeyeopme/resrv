import io.spring.gradle.dependencymanagement.DependencyManagementPlugin
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.openrewrite.gradle.RewriteExtension
import org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES

val jacocoLineCoverageMinimums =
    mapOf(
        "shared-kernel" to "0.85".toBigDecimal(),
        "platform" to "0.80".toBigDecimal(),
        "timeslot" to "0.80".toBigDecimal(),
    )

val jacocoPackageLineCoverageMinimums =
    mapOf(
        "platform" to
            mapOf(
                "io.resrv.platform.application.*" to "0.90".toBigDecimal(),
                "io.resrv.platform.adapter.in.web.account" to "0.90".toBigDecimal(),
                "io.resrv.platform.adapter.in.web.auth" to "0.90".toBigDecimal(),
                "io.resrv.platform.adapter.in.web.business" to "0.90".toBigDecimal(),
                "io.resrv.platform.adapter.in.web.security" to "0.90".toBigDecimal(),
                "io.resrv.platform.adapter.out.persistence.account" to "0.90".toBigDecimal(),
                "io.resrv.platform.adapter.out.persistence.business" to "0.90".toBigDecimal(),
                "io.resrv.platform.adapter.out.persistence.membership" to "0.90".toBigDecimal(),
            ),
        "timeslot" to
            mapOf(
                "io.resrv.timeslot.application.reservation" to "0.85".toBigDecimal(),
                "io.resrv.timeslot.application.resource" to "0.90".toBigDecimal(),
                "io.resrv.timeslot.application.schedule" to "0.90".toBigDecimal(),
                "io.resrv.timeslot.application.settings" to "0.90".toBigDecimal(),
                "io.resrv.timeslot.application.slot" to "0.90".toBigDecimal(),
                "io.resrv.timeslot.adapter.in.web.reservation" to "0.60".toBigDecimal(),
                "io.resrv.timeslot.adapter.in.web.resource" to "0.90".toBigDecimal(),
                "io.resrv.timeslot.adapter.in.web.schedule" to "0.75".toBigDecimal(),
                "io.resrv.timeslot.adapter.in.web.security" to "0.85".toBigDecimal(),
                "io.resrv.timeslot.adapter.in.web.settings" to "0.90".toBigDecimal(),
                "io.resrv.timeslot.adapter.in.web.slot" to "0.90".toBigDecimal(),
                "io.resrv.timeslot.adapter.out.persistence.lock" to "0.90".toBigDecimal(),
                "io.resrv.timeslot.adapter.out.persistence.reservation" to "0.90".toBigDecimal(),
                "io.resrv.timeslot.adapter.out.persistence.resource" to "0.90".toBigDecimal(),
                "io.resrv.timeslot.adapter.out.persistence.schedule" to "0.90".toBigDecimal(),
                "io.resrv.timeslot.adapter.out.persistence.settings" to "0.90".toBigDecimal(),
            ),
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

                jacocoPackageLineCoverageMinimums[project.name]?.forEach {
                    (packagePattern, minimumCoverage) ->
                    rule {
                        element = "PACKAGE"
                        includes = listOf(packagePattern)
                        limit {
                            counter = "LINE"
                            value = "COVEREDRATIO"
                            minimum = minimumCoverage
                        }
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
