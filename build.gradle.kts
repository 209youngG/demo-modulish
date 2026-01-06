plugins {
    java
    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com"
version = "0.0.1-SNAPSHOT"
description = "demo-modulish"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

val springModulithVersion = "2.0.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.springframework.modulith:spring-modulith-events-api")
    
    // Transactional Outbox & JDBC
    implementation("org.springframework.modulith:spring-modulith-starter-jdbc")
    
    // Database
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2") // Keep H2 for fallback/tests if needed

    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.3")

    // Reliability
    implementation("org.springframework.retry:spring-retry:2.0.10")
    implementation("org.springframework:spring-aspects")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.modulith:spring-modulith-bom:$springModulithVersion")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
