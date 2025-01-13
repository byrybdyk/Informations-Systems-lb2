plugins {
	java
	id("org.springframework.boot") version "3.3.4"
	id("io.spring.dependency-management") version "1.1.6"
}

group = "com.byrybdyk"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.postgresql:postgresql")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-websocket")

	implementation ("org.springframework.boot:spring-boot-starter-oauth2-client")
	implementation ("org.springframework.security:spring-security-oauth2-client")

	implementation ("org.springframework.security:spring-security-oauth2-client")
	implementation ("org.springframework.security:spring-security-oauth2-jose")


	implementation("org.keycloak:keycloak-spring-boot-starter:25.0.3")
	implementation("org.springframework.security:spring-security-oauth2-jose")
	implementation("org.keycloak:keycloak-admin-client:22.0.5")
	implementation("org.keycloak:keycloak-core:22.0.5")

	implementation("org.apache.poi:poi-ooxml:5.2.3")

	implementation("io.jsonwebtoken:jjwt-api:0.11.5")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


tasks.withType<Test> {
	useJUnitPlatform()
}
