plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "resrv"

include(
    "shared-kernel",
    "platform-domain",
    "platform-application",
    "platform-adapter-persistence",
    "platform-adapter-web",
    "platform-api",
    "timeslot-domain",
    "timeslot-application",
    "timeslot-adapter-persistence",
    "timeslot-adapter-web",
    "timeslot-booking-api",
)
