plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "resrv"

include(
    "domain",
    "application",
    "adapter-web",
    "adapter-persistence",
    "bootstrap",
)
