plugins {
    kotlin("jvm") version "1.9.23" apply false
    kotlin("kapt") version "1.9.23" apply false
    kotlin("plugin.spring") version "1.9.23" apply false
    kotlin("plugin.jpa") version "1.9.23" apply false

    id("org.springframework.boot") version "3.2.5" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
}

allprojects {
    group = "com.ogonggo"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
