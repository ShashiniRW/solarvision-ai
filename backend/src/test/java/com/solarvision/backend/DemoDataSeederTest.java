package com.solarvision.backend;

import com.solarvision.backend.demo.DemoDataSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Starts the whole app against a real PostgreSQL (Testcontainers) with the demo seeder switched on,
 * then checks the data and that a second run adds nothing.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
    "app.demo.enabled=true",
    "app.demo.password=test-only-demo-password",
    "app.simulator.enabled=false"
})
class DemoDataSeederTest {

    @Autowired
    private DemoDataSeeder seeder;

    @Autowired
    private JdbcTemplate jdbc;

    private long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value != null ? value : 0;
    }

    private long demoEquipment() {
        return count("""
                SELECT COUNT(*) FROM equipment e
                JOIN sites s ON s.id = e.site_id
                JOIN projects p ON p.id = s.project_id
                JOIN organizations o ON o.id = p.organization_id
                WHERE o.name = 'SolarVision Energy (Demo)'
                """);
    }

    @Test
    void createsTheDemoDataOnce_andASecondRunAddsNothing() throws Exception {
        // the seeder already ran when the app started
        assertThat(count("SELECT COUNT(*) FROM organizations WHERE name = 'SolarVision Energy (Demo)'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM users WHERE email LIKE 'demo.%@example.com'")).isEqualTo(4);
        assertThat(demoEquipment()).isEqualTo(37);
        assertThat(count("SELECT COUNT(*) FROM sensor_readings")).isGreaterThanOrEqualTo(30L * 96 * 7);
        assertThat(count("SELECT COUNT(*) FROM alerts")).isEqualTo(5);
        assertThat(count("SELECT COUNT(*) FROM inspections")).isEqualTo(6);

        long readingsBefore = count("SELECT COUNT(*) FROM sensor_readings");

        // running it again must not create anything
        seeder.run(null);

        assertThat(count("SELECT COUNT(*) FROM organizations WHERE name = 'SolarVision Energy (Demo)'")).isEqualTo(1);
        assertThat(demoEquipment()).isEqualTo(37);
        assertThat(count("SELECT COUNT(*) FROM sensor_readings")).isEqualTo(readingsBefore);
    }
}
