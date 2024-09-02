plugins {
	java
	id("org.springframework.boot") version "3.3.3"
	id("io.spring.dependency-management") version "1.1.6"
	kotlin("jvm")
}

group = "br.ufc.quixada.projetoServerEsgrima"
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
	implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
	implementation("org.springframework.boot:spring-boot-starter-json:3.3.3")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	implementation("org.xerial:sqlite-jdbc:3.46.1.0")
	implementation("org.hibernate.orm:hibernate-community-dialects:6.6.0.Final")
	compileOnly("org.projectlombok:lombok:1.18.34")
	annotationProcessor("org.projectlombok:lombok:1.18.34")
	implementation(kotlin("stdlib-jdk8"))

}

tasks.withType<Test> {
	useJUnitPlatform()
}
