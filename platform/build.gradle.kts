plugins {
    java
    checkstyle
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dependency.management)
    alias(libs.plugins.jib)
    jacoco
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveBaseName.set("resrv-platform-api")
}

tasks.named<Jar>("jar") {
    enabled = true
}

// Run from the repository root so Spring Boot Docker Compose can discover ./compose.yml.
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    workingDir = rootProject.projectDir
    if (System.getenv("SPRING_PROFILES_ACTIVE").isNullOrBlank()) {
        environment("SPRING_PROFILES_ACTIVE", "local")
    }
}

jib {
    from {
        image = "eclipse-temurin:25-jre"
    }
    to {
        image = "resrv-platform-api:latest"
    }
    container {
        mainClass = "io.resrv.platform.api.PlatformApiApplication"
        ports = listOf("8080")
    }
}

dependencies {
    implementation(project(":shared-kernel"))
    implementation(project(":platform-exchange"))
    implementation(project(":ticketing"))
    implementation(project(":timeslot"))

    implementation(libs.spring.tx)
    implementation(libs.spring.context)
    implementation(libs.spring.boot.starter.mail)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.bcprov)
    implementation(libs.jackson.databind)
    compileOnly(libs.swagger.annotations.jakarta)

    runtimeOnly(libs.postgresql)

    developmentOnly(libs.spring.boot.docker.compose)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.spring.boot.starter.flyway.test)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.archunit.junit5)
}
