import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// 두 API가 같은 형식으로 로그를 남기게 하는 설정 모듈.
// 코드는 없고 logback-spring.xml과 JSON 인코더 의존성만 제공한다. 로그를 찍는 쪽은 SLF4J만 쓴다.
plugins {
    kotlin("jvm")
    id("io.spring.dependency-management")
}

kotlin {
    jvmToolchain(17)
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.2.5")
    }
}

dependencies {
    // 8.x는 logback 1.5가 필요하다. Spring Boot 3.2는 logback 1.4를 쓴다.
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    // logback-spring.xml의 <if>가 LOG_FORMAT 값으로 출력 형식을 고를 때 쓴다.
    implementation("org.codehaus.janino:janino")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}
