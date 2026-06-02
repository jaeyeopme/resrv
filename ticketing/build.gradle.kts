plugins {
    java
    checkstyle
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dependency.management)
    jacoco
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    enabled = false
}

dependencies {
    implementation(project(":shared-kernel"))
    implementation(project(":platform-exchange"))

    implementation(libs.spring.tx)
    implementation(libs.spring.context)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.flyway.database.postgresql)

    runtimeOnly(libs.postgresql)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.spring.boot.starter.flyway.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.archunit.junit5)
}
