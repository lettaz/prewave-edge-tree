package com.amaizu.edgetree

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.postgresql.PostgreSQLContainer

// A throwaway PostgreSQL for database tests, so they never touch the local edge_tree data.
@TestConfiguration(proxyBeanMethods = false)
class PostgresTestConfiguration {

    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer = PostgreSQLContainer("postgres:18.6")
}
