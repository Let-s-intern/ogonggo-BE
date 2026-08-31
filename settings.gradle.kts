pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "ogonggo-server"

include(
    "ogonggo-core",
    "ogonggo-api-user",
    "ogonggo-api-admin",
)
