import java.util.Properties

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
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	implementation("org.springframework.boot:spring-boot-starter-jooq")
    runtimeOnly("org.postgresql:postgresql")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	runtimeOnly("org.flywaydb:flyway-database-postgresql")
	jooqCodegen("org.postgresql:postgresql")
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
}

val codegenEnv = Properties().apply {
    val envFile = file(".env")

    if (envFile.isFile) {
        envFile.inputStream().use { load(it) }
    }
}

jooq {
    configuration {
        jdbc {
            driver = "org.postgresql.Driver"
            url = System.getenv("DB_URL")
                ?: codegenEnv.getProperty("DB_URL")
            user = System.getenv("DB_USERNAME")
                ?: codegenEnv.getProperty("DB_USERNAME")
            password = System.getenv("DB_PASSWORD")
                ?: codegenEnv.getProperty("DB_PASSWORD")
        }
		generator {
			name = "org.jooq.codegen.KotlinGenerator"

			database {
				name = "org.jooq.meta.postgres.PostgresDatabase"
				inputSchema = "public"
				includes = ".*"
    			excludes = "flyway_schema_history"
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

