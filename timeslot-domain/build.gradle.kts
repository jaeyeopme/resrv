plugins {
    `java-library`
    checkstyle
    alias(libs.plugins.dependency.management)
    jacoco
}

dependencies {
    api(project(":shared-kernel"))

    testImplementation(libs.spring.boot.starter.test)
}
