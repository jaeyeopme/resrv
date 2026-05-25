plugins {
    `java-library`
    checkstyle
    alias(libs.plugins.dependency.management)
    jacoco
}

dependencies {
    implementation(project(":timeslot-application"))
    implementation(project(":timeslot-domain"))
    implementation(project(":shared-kernel"))
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    compileOnly(libs.swagger.annotations.jakarta)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.security.test)
}
