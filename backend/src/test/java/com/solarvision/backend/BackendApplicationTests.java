package com.solarvision.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Starts the whole application against a fresh PostgreSQL in Docker.
 * Proves that all Flyway migrations run on an empty database and that
 * every JPA entity matches the resulting schema.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }
}
