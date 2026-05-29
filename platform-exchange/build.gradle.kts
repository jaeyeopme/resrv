plugins {
    `java-library`
    checkstyle
    alias(libs.plugins.dependency.management)
}

dependencies {
    api(project(":shared-kernel"))

    testImplementation(libs.archunit.junit5)
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
