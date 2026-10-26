plugins {
	kotlin("jvm") version "2.3.21"
	kotlin("plugin.spring") version "2.3.21"
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.jooq.jooq-codegen-gradle") version "3.21.7"
}

group = "com.amaizu"
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
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	implementation("org.springframework.boot:spring-boot-starter-jooq")
    runtimeOnly("org.postgresql:postgresql")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	runtimeOnly("org.flywaydb:flyway-database-postgresql")
	jooqCodegen("org.jooq:jooq-meta-extensions:3.21.7")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0")

	// Testing
	testImplementation("org.springframework.boot:spring-boot-starter-jooq-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}

	sourceSets.named("main") {
        kotlin.srcDir(
            layout.buildDirectory.dir("generated-src/jooq")
        )
    }
}

tasks.withType<Test> {
	useJUnitPlatform()

	// Print each test result and a summary in the terminal.
	testLogging {
		events("passed", "skipped", "failed")
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
	}

	afterSuite(KotlinClosure2<TestDescriptor, TestResult, Unit>({ suite, result ->
		if (suite.parent == null) {
			println(
				"\nTests: ${result.testCount} total, ${result.successfulTestCount} passed, " +
					"${result.failedTestCount} failed, ${result.skippedTestCount} skipped"
			)
		}
	}))
}

val migrations = layout.projectDirectory.dir("src/main/resources/db/migration")

// Generates the jOOQ classes from the Flyway migration files, so building needs no running database.
jooq {
    configuration {
		generator {
			name = "org.jooq.codegen.KotlinGenerator"

			database {
				name = "org.jooq.meta.extensions.ddl.DDLDatabase"
				properties {
					property {
						key = "scripts"
						value = migrations.asFile.absolutePath + "/*.sql"
					}
					property {
						key = "sort"
						value = "flyway"
					}
					property {
						key = "defaultNameCase"
						value = "lower"
					}
				}
			}

			target {
				packageName = "com.amaizu.edgetree.generated"
				directory = layout.buildDirectory
					.dir("generated-src/jooq")
					.get()
					.asFile
					.absolutePath
			}
    	}
	}
}

// Regenerate when a migration changes, and always before compiling.
tasks.named("jooqCodegen") {
	inputs.files(fileTree(migrations))
}

tasks.named("compileKotlin") {
	dependsOn("jooqCodegen")
}

