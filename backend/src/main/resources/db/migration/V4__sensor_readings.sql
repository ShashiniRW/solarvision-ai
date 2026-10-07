-- V4: Sensor readings (Sprint 5 - Monitoring)
-- One row = one measurement from one piece of equipment at one moment.

CREATE TABLE sensor_readings (
                                 id              BIGSERIAL PRIMARY KEY,
                                 equipment_id    BIGINT        NOT NULL,
                                 recorded_at     TIMESTAMPTZ   NOT NULL,
                                 power_kw        NUMERIC(10,3),
                                 energy_kwh      NUMERIC(12,3),
                                 voltage_v       NUMERIC(8,2),
                                 current_a       NUMERIC(8,2),
                                 temperature_c   NUMERIC(5,2),
                                 is_simulated    BOOLEAN       NOT NULL DEFAULT TRUE,
                                 created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

                                 CONSTRAINT fk_sensor_readings_equipment
                                     FOREIGN KEY (equipment_id) REFERENCES equipment (id) ON DELETE CASCADE,

    -- the same equipment cannot have two readings at the exact same moment
                                 CONSTRAINT uq_sensor_readings_equipment_time
                                     UNIQUE (equipment_id, recorded_at),

                                 CONSTRAINT chk_sensor_readings_power_nonneg  CHECK (power_kw IS NULL OR power_kw >= 0),
                                 CONSTRAINT chk_sensor_readings_energy_nonneg CHECK (energy_kwh IS NULL OR energy_kwh >= 0)
);

-- Speeds up "latest reading" and "readings between two times" queries
CREATE INDEX idx_sensor_readings_equipment_time
    ON sensor_readings (equipment_id, recorded_at DESC);