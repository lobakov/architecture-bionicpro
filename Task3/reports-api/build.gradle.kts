import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17

plugins {
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
}

group = "com.bionicpro"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("com.clickhouse:clickhouse-jdbc:0.4.6")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.minio:minio:8.5.7")
}

tasks.compileKotlin {
    compilerOptions {
        freeCompilerArgs.set(listOf("-Xjsr305=strict"))
        jvmTarget.set(JVM_17)
    }
}

tasks.compileJava {
    enabled = false
}

tasks.compileTestJava {
    enabled = false
}

tasks.jar {
    enabled = false
}

tasks.bootJar {
    enabled = true
}

tasks.test {
    useJUnitPlatform()
}
