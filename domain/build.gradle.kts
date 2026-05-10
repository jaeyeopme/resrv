plugins {
    `java-library`
    checkstyle
    alias(libs.plugins.dependency.management)
    jacoco
}

dependencies {
    api(libs.java.uuid.generator)

    testImplementation(libs.spring.boot.starter.test)
}
