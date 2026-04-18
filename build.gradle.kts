import io.spring.gradle.dependencymanagement.DependencyManagementPlugin
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES

val jacocoLineCoverageMinimums =
    mapOf(
        "domain" to "0.85".toBigDecimal(),
        "application" to "0.90".toBigDecimal(),
        "adapter-web" to "0.90".toBigDecimal(),
        "adapter-persistence" to "0.90".toBigDecimal(),
        "bootstrap" to "0.90".toBigDecimal(),
    )

plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.dependency.management) apply false
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

        val checkNullMarkedPackageInfo =
            tasks.register("checkNullMarkedPackageInfo") {
                group = LifecycleBasePlugin.VERIFICATION_GROUP
                description =
                    "Verifies every production Java package has package-info.java with NullMarked."

                doLast {
                    val sourceRoot = layout.projectDirectory.dir("src/main/java").asFile
                    if (!sourceRoot.exists()) {
                        return@doLast
                    }

                    val missingPackages =
                        sourceRoot
                            .walkTopDown()
                            .filter { file ->
                                file.isFile
                                    && file.extension == "java"
                                    && file.name != "package-info.java"
                            }
                            .map { it.parentFile }
                            .distinct()
                            .filter { packageDir ->
                                val packageInfo = packageDir.resolve("package-info.java")
                                !packageInfo.isFile || !packageInfo.readText().contains("NullMarked")
                            }
                            .map { it.relativeTo(projectDir).path }
                            .sorted()
                            .toList()

                    if (missingPackages.isNotEmpty()) {
                        throw GradleException(
                            "Production packages must declare package-info.java with NullMarked:\n"
                                + missingPackages.joinToString("\n"))
                    }
                }
            }

        tasks.named("check") {
            dependsOn(checkNullMarkedPackageInfo)
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
