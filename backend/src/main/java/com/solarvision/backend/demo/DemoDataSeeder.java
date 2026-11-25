package com.solarvision.backend.demo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Fills the system with realistic demo data for the solar company: staff and customer accounts,
 * projects, sites, equipment, 30 days of readings, alerts and inspections.
 * Runs at startup ONLY when app.demo.enabled=true, and only once (it skips when the demo
 * organization already exists). Everything happens in one transaction: all or nothing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    public static final String DEMO_ORGANIZATION = "SolarVision Energy (Demo)";

    // the demo accounts (example.com is reserved for examples, so it never belongs to a real person)
    public static final String MANAGER_EMAIL = "demo.manager@example.com";
    public static final String ENGINEER_EMAIL = "demo.engineer@example.com";
    public static final String TECHNICIAN_EMAIL = "demo.technician@example.com";
    public static final String CUSTOMER_EMAIL = "demo.customer@example.com";

    private static final ZoneId SITE_ZONE = ZoneId.of("Asia/Colombo");
    private static final int HISTORY_DAYS = 30;
    private static final Duration READING_INTERVAL = Duration.ofMinutes(15);
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo.password:}")
    private String demoPassword;

    private final Random random = new Random(42); // fixed seed: the same demo every time
    private int equipmentCounter = 0;

    /** An inverter that gets reading history. A "degrading" one slowly loses output in the last week. */
    private record DemoInverter(long id, double capacityKw, boolean degrading) {
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Integer existing = jdbc.queryForObject(
            "SELECT COUNT(*) FROM organizations WHERE name = ?", Integer.class, DEMO_ORGANIZATION);
        if (existing != null && existing > 0) {
            log.info("Demo data already exists - nothing to do");
            return;
        }
        if (demoPassword == null || demoPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                "app.demo.enabled=true needs app.demo.password (at least " + MIN_PASSWORD_LENGTH + " characters)");
        }

        OffsetDateTime now = OffsetDateTime.now(SITE_ZONE);
        LocalDate today = now.toLocalDate();

        // ---------- the company and its people ----------
        long org = insertId("""
                INSERT INTO organizations (name, email, phone, address)
                VALUES (?, ?, ?, ?) RETURNING id
                """, DEMO_ORGANIZATION, "hello@example.com", "+94 11 000 0000", "Colombo, Sri Lanka");

        String passwordHash = passwordEncoder.encode(demoPassword);
        long manager = user(org, "Nimal", "Perera", MANAGER_EMAIL, passwordHash, "MANAGER");
        long engineer = user(org, "Kasun", "Silva", ENGINEER_EMAIL, passwordHash, "ENGINEER");
        long technician = user(org, "Tharindu", "Fernando", TECHNICIAN_EMAIL, passwordHash, "TECHNICIAN");
        long customer = user(org, "Dilani", "Jayawardena", CUSTOMER_EMAIL, passwordHash, "CUSTOMER");

        // ---------- projects ----------
        long officeProject = project(org, customer, "Colombo Office Rooftops",
            "Rooftop systems on the head office and the main warehouse.", "COMPLETED", today.minusMonths(14));
        long farmProject = project(org, null, "Hambantota Solar Farm",
            "Ground-mounted solar farm in the dry zone.", "COMPLETED", today.minusMonths(9));
        long hotelProject = project(org, null, "Kandy Hillside Hotel",
            "Rooftop system with battery backup for a hotel.", "IN_PROGRESS", today.minusMonths(3));

        assign(officeProject, manager, "MANAGER");
        assign(farmProject, manager, "MANAGER");
        assign(hotelProject, manager, "MANAGER");
        assign(officeProject, engineer, "ENGINEER");
        assign(farmProject, engineer, "ENGINEER");
        assign(officeProject, technician, "TECHNICIAN");
        assign(farmProject, technician, "TECHNICIAN");
        assign(hotelProject, technician, "TECHNICIAN");

        // ---------- sites ----------
        long headOffice = site(officeProject, "Head Office Rooftop", "Fort, Colombo 01", 6.9344, 79.8428, 60);
        long warehouse = site(officeProject, "Peliyagoda Warehouse", "Negombo Road, Peliyagoda", 6.9637, 79.8826, 40);
        long farm = site(farmProject, "Hambantota Field A", "Sooriyawewa Road, Hambantota", 6.1832, 81.0921, 225);
        long hotel = site(hotelProject, "Hotel Main Roof", "Lake Round, Kandy", 7.2915, 80.6413, 50);

        // ---------- equipment ----------
        List<DemoInverter> inverters = new ArrayList<>();
        LocalDate officeInstalled = today.minusMonths(13);
        LocalDate farmInstalled = today.minusMonths(8);
        LocalDate hotelInstalled = today.minusMonths(2);

        // Head office: 6 strings x 10 kW, 2 x 30 kW inverters, battery, meter
        for (int i = 0; i < 6; i++) {
            panel(headOffice, 10, "ACTIVE", officeInstalled);
        }
        long officeInverter1 = inverter(headOffice, "SMA", "Sunny Tripower 30", 30, officeInstalled);
        long officeInverter2 = inverter(headOffice, "SMA", "Sunny Tripower 30", 30, officeInstalled);
        inverters.add(new DemoInverter(officeInverter1, 30, false));
        inverters.add(new DemoInverter(officeInverter2, 30, false));
        equipment(headOffice, "BATTERY", "BYD", "Battery-Box Premium HVS 10.2", decimal(5), "ACTIVE", officeInstalled, 10);
        meter(headOffice, officeInstalled);

        // Warehouse: 4 strings x 10 kW, 1 x 40 kW inverter, meter
        for (int i = 0; i < 4; i++) {
            panel(warehouse, 10, "ACTIVE", officeInstalled);
        }
        long warehouseInverter = inverter(warehouse, "Huawei", "SUN2000-40KTL", 40, officeInstalled);
        inverters.add(new DemoInverter(warehouseInverter, 40, false));
        long warehouseMeter = meter(warehouse, officeInstalled);

        // Farm: 9 strings x 25 kW (one faulty), 3 x 75 kW inverters (one slowly underperforming), sensor, meter
        long faultyPanel = 0;
        for (int i = 0; i < 9; i++) {
            long panel = panel(farm, 25, i == 4 ? "FAULTY" : "ACTIVE", farmInstalled);
            if (i == 4) {
                faultyPanel = panel;
            }
        }
        long farmInverter1 = inverter(farm, "Sungrow", "SG75CX", 75, farmInstalled);
        long farmInverter2 = inverter(farm, "Sungrow", "SG75CX", 75, farmInstalled);
        long farmInverter3 = inverter(farm, "Sungrow", "SG75CX", 75, farmInstalled);
        inverters.add(new DemoInverter(farmInverter1, 75, false));
        inverters.add(new DemoInverter(farmInverter2, 75, false));
        inverters.add(new DemoInverter(farmInverter3, 75, true));
        equipment(farm, "SENSOR", "Kipp & Zonen", "SMP10 pyranometer", null, "ACTIVE", farmInstalled, 5);
        meter(farm, farmInstalled);

        // Hotel: 4 strings x 12.5 kW, 1 x 50 kW inverter, battery, meter
        for (int i = 0; i < 4; i++) {
            panel(hotel, 12.5, "ACTIVE", hotelInstalled);
        }
        long hotelInverter = inverter(hotel, "Fronius", "Tauro 50-3", 50, hotelInstalled);
        inverters.add(new DemoInverter(hotelInverter, 50, false));
        long hotelBattery = equipment(hotel, "BATTERY", "BYD", "Battery-Box Premium HVS 12.8",
            decimal(6), "ACTIVE", hotelInstalled, 10);
        meter(hotel, hotelInstalled);

        // ---------- 30 days of readings ----------
        int readings = insertReadingHistory(inverters, now);

        // ---------- alerts (open, acknowledged and resolved) ----------
        long farmHeatAlert = alert(farmInverter2, "OVER_TEMPERATURE", "CRITICAL", "OPEN",
            "Temperature 81.40 C exceeds the critical limit of 75 C", now.minusHours(2), null, null, null, null);
        alert(farmInverter3, "SUSTAINED_UNDERPERFORMANCE", "WARNING", "ACKNOWLEDGED",
            "Produced about 55% of the expected energy for 7 days in a row", now.minusDays(1),
            now.minusHours(20), engineer, null, null);
        alert(hotelInverter, "VOLTAGE_OUT_OF_RANGE", "WARNING", "OPEN",
            "Voltage 258.40 V is outside the normal range of 207 to 253 V", now.minusHours(5), null, null, null, null);
        long officeOutage = alert(officeInverter1, "ZERO_POWER", "CRITICAL", "RESOLVED",
            "No power produced during daylight hours", now.minusDays(12),
            now.minusDays(12).plusMinutes(20), technician, now.minusDays(12).plusHours(3), technician);
        alert(warehouseInverter, "OVER_TEMPERATURE", "WARNING", "RESOLVED",
            "Temperature 68.20 C is above the warning limit of 65 C", now.minusDays(20),
            now.minusDays(20).plusHours(1), engineer, now.minusDays(19), engineer);

        // ---------- inspections (completed, in progress, scheduled, overdue) ----------
        inspection(officeInverter2, null, "ROUTINE", "COMPLETED", now.minusDays(25), technician, manager,
            now.minusDays(25), now.minusDays(25).plusHours(2), "PASSED",
            "Annual check: connections tight, no hot spots on the thermal scan.");
        inspection(officeInverter1, officeOutage, "ALERT_FOLLOW_UP", "COMPLETED", now.minusDays(12), technician, manager,
            now.minusDays(12).plusMinutes(40), now.minusDays(12).plusHours(3), "PASSED",
            "AC breaker had tripped after a grid fault. Reset and monitored for two hours.");
        inspection(farmInverter2, farmHeatAlert, "ALERT_FOLLOW_UP", "IN_PROGRESS", now.minusHours(1), technician, manager,
            now.minusMinutes(30), null, null, null);
        inspection(faultyPanel, null, "ROUTINE", "SCHEDULED",
            today.plusDays(1).atTime(8, 0).atZone(SITE_ZONE).toOffsetDateTime(),
            technician, engineer, null, null, null, null);
        inspection(hotelBattery, null, "WARRANTY_CHECK", "SCHEDULED",
            today.plusDays(5).atTime(10, 0).atZone(SITE_ZONE).toOffsetDateTime(),
            technician, manager, null, null, null, null);
        inspection(warehouseMeter, null, "ROUTINE", "SCHEDULED", now.minusDays(2), technician, manager,
            null, null, null, null); // overdue: it should have happened 2 days ago

        log.info("Demo data created: 4 sites, {} equipment, {} readings", equipmentCounter, readings);
    }

    // ---------- readings ----------

    /** One reading every 15 minutes for every inverter, following the sun and the daily weather. */
    private int insertReadingHistory(List<DemoInverter> inverters, OffsetDateTime now) {
        ZonedDateTime start = now.atZoneSameInstant(SITE_ZONE).toLocalDate()
            .minusDays(HISTORY_DAYS).atStartOfDay(SITE_ZONE);

        // each day gets its own weather: 0.45 = very cloudy ... 1.0 = clear sky
        double[] weather = new double[HISTORY_DAYS + 1];
        for (int day = 0; day <= HISTORY_DAYS; day++) {
            weather[day] = 0.45 + 0.55 * random.nextDouble();
        }

        List<Object[]> rows = new ArrayList<>();
        for (DemoInverter inverter : inverters) {
            for (ZonedDateTime time = start; time.toOffsetDateTime().isBefore(now); time = time.plus(READING_INTERVAL)) {
                int day = (int) ChronoUnit.DAYS.between(start.toLocalDate(), time.toLocalDate());
                double hour = time.getHour() + time.getMinute() / 60.0;
                double sun = hour > 6 && hour < 18 ? Math.sin(Math.PI * (hour - 6) / 12) : 0;
                double health = inverter.degrading() && day >= HISTORY_DAYS - 7 ? 0.55 : 1.0;
                double load = sun * weather[day] * (0.9 + 0.1 * random.nextDouble()) * health;

                double powerKw = inverter.capacityKw() * load;
                double energyKwh = powerKw * READING_INTERVAL.toMinutes() / 60.0;
                double voltageV = 230 + (random.nextDouble() - 0.5) * 6;
                double currentA = powerKw * 1000 / voltageV;
                double temperatureC = 26 + 22 * load + (random.nextDouble() - 0.5) * 2;

                rows.add(new Object[]{inverter.id(), time.toOffsetDateTime(),
                    round(powerKw, 3), round(energyKwh, 3), round(voltageV, 2),
                    round(currentA, 2), round(temperatureC, 2)});
            }
        }

        int[][] results = jdbc.batchUpdate("""
                INSERT INTO sensor_readings (equipment_id, recorded_at, power_kw, energy_kwh,
                                             voltage_v, current_a, temperature_c, is_simulated)
                VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)
                ON CONFLICT (equipment_id, recorded_at) DO NOTHING
                """, rows, 1000, (statement, row) -> {
            for (int i = 0; i < row.length; i++) {
                statement.setObject(i + 1, row[i]);
            }
        });
        return Arrays.stream(results).flatMapToInt(Arrays::stream).map(count -> Math.max(count, 0)).sum();
    }

    // ---------- insert helpers ----------

    private long user(long org, String firstName, String lastName, String email, String passwordHash, String role) {
        long id = insertId("""
                INSERT INTO users (organization_id, first_name, last_name, email, password_hash)
                VALUES (?, ?, ?, ?, ?) RETURNING id
                """, org, firstName, lastName, email, passwordHash);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) SELECT ?, id FROM roles WHERE name = ?", id, role);
        return id;
    }

    private long project(long org, Long customer, String name, String description, String status, LocalDate start) {
        return insertId("""
                INSERT INTO projects (organization_id, customer_id, name, description, status, start_date)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING id
                """, org, id(customer), name, description, status, start);
    }

    private void assign(long project, long user, String roleInProject) {
        jdbc.update("INSERT INTO project_assignments (project_id, user_id, role_in_project) VALUES (?, ?, ?)",
            project, user, roleInProject);
    }

    private long site(long project, String name, String address, double latitude, double longitude, double capacityKw) {
        return insertId("""
                INSERT INTO sites (project_id, name, address, latitude, longitude, capacity_kw)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING id
                """, project, name, address, round(latitude, 6), round(longitude, 6), decimal(capacityKw));
    }

    private long panel(long site, double capacityKw, String status, LocalDate installed) {
        return equipment(site, "PANEL", "Jinko Solar", "Tiger Neo 580 W string", decimal(capacityKw), status, installed, 25);
    }

    private long inverter(long site, String manufacturer, String model, double capacityKw, LocalDate installed) {
        return equipment(site, "INVERTER", manufacturer, model, decimal(capacityKw), "ACTIVE", installed, 10);
    }

    private long meter(long site, LocalDate installed) {
        return equipment(site, "METER", "Schneider Electric", "iEM3255 energy meter", null, "ACTIVE", installed, 5);
    }

    private long equipment(long site, String type, String manufacturer, String model, BigDecimal capacityKw,
                           String status, LocalDate installed, int warrantyYears) {
        equipmentCounter++;
        String serialNumber = "SVD-%s-%04d".formatted(type.substring(0, 3), equipmentCounter);
        String qrCode = "QR-DEMO-%04d".formatted(equipmentCounter);
        return insertId("""
                INSERT INTO equipment (site_id, equipment_type, manufacturer, model, serial_number, qr_code,
                                       capacity_kw, installation_date, warranty_expiry_date, warranty_years, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, site, type, manufacturer, model, serialNumber, qrCode,
            new SqlParameterValue(Types.NUMERIC, capacityKw), installed,
            installed.plusYears(warrantyYears), warrantyYears, status);
    }

    private long alert(long equipment, String type, String severity, String status, String message,
                       OffsetDateTime triggeredAt, OffsetDateTime acknowledgedAt, Long acknowledgedBy,
                       OffsetDateTime resolvedAt, Long resolvedBy) {
        return insertId("""
                INSERT INTO alerts (equipment_id, alert_type, severity, status, message, triggered_at,
                                    acknowledged_at, acknowledged_by, resolved_at, resolved_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, equipment, type, severity, status, message, triggeredAt,
            time(acknowledgedAt), id(acknowledgedBy), time(resolvedAt), id(resolvedBy));
    }

    private void inspection(long equipment, Long alert, String type, String status, OffsetDateTime scheduledAt,
                            long assignedTo, long createdBy, OffsetDateTime startedAt, OffsetDateTime completedAt,
                            String result, String findings) {
        jdbc.update("""
                INSERT INTO inspections (equipment_id, alert_id, inspection_type, status, scheduled_at, assigned_to,
                                         created_by, started_at, completed_at, result, findings)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, equipment, id(alert), type, status, scheduledAt, assignedTo, createdBy,
            time(startedAt), time(completedAt),
            new SqlParameterValue(Types.VARCHAR, result), new SqlParameterValue(Types.VARCHAR, findings));
    }

    private long insertId(String sql, Object... args) {
        return Objects.requireNonNull(jdbc.queryForObject(sql, Long.class, args));
    }

    // typed values, so the database also understands NULLs
    private static SqlParameterValue id(Long value) {
        return new SqlParameterValue(Types.BIGINT, value);
    }

    private static SqlParameterValue time(OffsetDateTime value) {
        return new SqlParameterValue(Types.TIMESTAMP_WITH_TIMEZONE, value);
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }
}
