pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "ogonggo-server"

include(
    "ogonggo-core",
    "ogonggo-logging",
    "ogonggo-api-user",
    "ogonggo-api-admin",
)
