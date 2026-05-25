plugins {
    `java-library`
    checkstyle
    alias(libs.plugins.dependency.management)
    jacoco
}

dependencies {
    implementation(project(":platform-domain"))

    implementation(libs.spring.tx)
    implementation(libs.spring.context)

    testImplementation(libs.spring.boot.starter.test)
}
