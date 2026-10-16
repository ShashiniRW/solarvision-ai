-- V5: Alerts (Sprint 6 - Maintenance & Alerts)
-- One row = one problem detected on one piece of equipment.

CREATE TABLE alerts (
                        id               BIGSERIAL PRIMARY KEY,
                        equipment_id     BIGINT        NOT NULL,
                        reading_id       BIGINT,
                        alert_type       VARCHAR(30)   NOT NULL,
                        severity         VARCHAR(10)   NOT NULL,
                        status           VARCHAR(15)   NOT NULL DEFAULT 'OPEN',
                        message          VARCHAR(500)  NOT NULL,
                        triggered_at     TIMESTAMPTZ   NOT NULL,
                        acknowledged_at  TIMESTAMPTZ,
                        acknowledged_by  BIGINT,
                        resolved_at      TIMESTAMPTZ,
                        resolved_by      BIGINT,
                        created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                        updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

                        CONSTRAINT fk_alerts_equipment
                            FOREIGN KEY (equipment_id) REFERENCES equipment (id) ON DELETE CASCADE,
                        CONSTRAINT fk_alerts_reading
                            FOREIGN KEY (reading_id) REFERENCES sensor_readings (id) ON DELETE SET NULL,
                        CONSTRAINT fk_alerts_acknowledged_by
                            FOREIGN KEY (acknowledged_by) REFERENCES users (id) ON DELETE SET NULL,
                        CONSTRAINT fk_alerts_resolved_by
                            FOREIGN KEY (resolved_by) REFERENCES users (id) ON DELETE SET NULL,

                        CONSTRAINT chk_alerts_type
                            CHECK (alert_type IN ('OVER_TEMPERATURE', 'ZERO_POWER', 'VOLTAGE_OUT_OF_RANGE')),
                        CONSTRAINT chk_alerts_severity
                            CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
                        CONSTRAINT chk_alerts_status
                            CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED'))
);

-- At most ONE unresolved alert per equipment and alert type (prevents alert flooding)
CREATE UNIQUE INDEX uq_alerts_active
    ON alerts (equipment_id, alert_type)
    WHERE status <> 'RESOLVED';

-- Speeds up "show me all open alerts, newest first" and "alerts for this equipment"
CREATE INDEX idx_alerts_status_triggered ON alerts (status, triggered_at DESC);
CREATE INDEX idx_alerts_equipment_triggered ON alerts (equipment_id, triggered_at DESC);