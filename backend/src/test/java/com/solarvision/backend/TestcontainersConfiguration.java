package com.solarvision.backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a fresh PostgreSQL 18 in Docker for integration tests.
 * @ServiceConnection tells Spring Boot to connect to it automatically
 * (URL, username and password), so no datasource settings are needed.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        // same major version as production (PostgreSQL 18)
        return new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));
    }
}
