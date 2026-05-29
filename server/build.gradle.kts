plugins {
    java
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.codeages"
version = "server"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-quartz")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Flyway
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-mysql")

    // MySQL
    runtimeOnly("com.mysql:mysql-connector-j")

    // RocketMQ
//    implementation("org.apache.rocketmq:rocketmq-spring-boot-starter:2.3.1")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.42")

    // MapStruct
    implementation("org.mapstruct:mapstruct:1.4.2.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.4.2.Final")

    // 注意：Lombok 和 MapStruct 一起使用时，如果遇到问题，需要添加这个绑定
    // annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    // Hibernate Types - 更新版本以兼容 Hibernate 6
//    implementation("com.vladmihalcea:hibernate-types-60:2.21.1")

    // Hutool
    implementation("cn.hutool:hutool-all:5.8.42")

    // Apache Commons
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.apache.commons:commons-text:1.10.0")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("commons-net:commons-net:3.11.1")

    // JWT
    implementation("com.auth0:java-jwt:3.19.0")

    // SSH
    implementation("com.hierynomus:sshj:0.40.0")

    // MVEL
    implementation("org.mvel:mvel2:2.5.0.Final")

    // Dinger
    implementation("com.github.answerail:dinger-spring-boot-starter:2.1.0")

    // Druid
    implementation("com.alibaba:druid:1.2.8")

    // Guacamole
    implementation("org.apache.guacamole:guacamole-common:1.5.5")

    // JetBrains Annotations
    implementation("org.jetbrains:annotations:17.0.0")

    // DingTalk
    implementation("com.dingtalk.open:dingtalk-stream:1.3.7")
    implementation("com.aliyun:alibaba-dingtalk-service-sdk:2.0.0")
    implementation("com.aliyun:dingtalk:2.1.65")

    // IP Region
    implementation("org.lionsoul:ip2region:2.7.0")

    // Gson
    implementation("com.google.code.gson:gson:2.11.0")

    // JAXB - 移除 javax，使用 jakarta
    implementation("jakarta.xml.bind:jakarta.xml.bind-api")
    implementation("org.glassfish.jaxb:jaxb-runtime")  // 让 Spring Boot 管理版本

    // Nashorn - JDK 15+ 已移除，需要独立依赖
    implementation("org.openjdk.nashorn:nashorn-core:15.4")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    // API Encrypt
    implementation("com.cxytiandi:monkey-api-encrypt-core:1.2.2.RELEASE")

    // Bouncy Castle
    implementation("org.bouncycastle:bcprov-jdk15to18:1.79")

    // ShedLock
    implementation("net.javacrumbs.shedlock:shedlock-spring:5.16.0")
    implementation("net.javacrumbs.shedlock:shedlock-provider-redis-spring:5.16.0")

    // AI - langchain4j
    implementation(platform("org.springframework.ai:spring-ai-bom:2.0.0-M2"))
    implementation("org.springframework.ai:spring-ai-starter-model-deepseek")
    implementation("org.springframework.ai:spring-ai-openai")
    implementation("org.springframework.ai:spring-ai-starter-mcp-client")
    implementation("org.springframework.boot:spring-boot-starter-actuator") //AI运行观察

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mockito:mockito-core:4.6.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
