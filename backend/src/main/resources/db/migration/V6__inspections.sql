-- V6: Inspections (Sprint 6 - Maintenance & Alerts)
-- One row = one inspection of one piece of equipment.

CREATE TABLE inspections (
                             id               BIGSERIAL PRIMARY KEY,
                             equipment_id     BIGINT        NOT NULL,
                             alert_id         BIGINT,
                             inspection_type  VARCHAR(20)   NOT NULL,
                             status           VARCHAR(15)   NOT NULL DEFAULT 'SCHEDULED',
                             scheduled_at     TIMESTAMPTZ   NOT NULL,
                             assigned_to      BIGINT,
                             created_by       BIGINT,
                             started_at       TIMESTAMPTZ,
                             completed_at     TIMESTAMPTZ,
                             result           VARCHAR(20),
                             findings         TEXT,
                             created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                             updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

                             CONSTRAINT fk_inspections_equipment
                                 FOREIGN KEY (equipment_id) REFERENCES equipment (id) ON DELETE CASCADE,
                             CONSTRAINT fk_inspections_alert
                                 FOREIGN KEY (alert_id) REFERENCES alerts (id) ON DELETE SET NULL,
                             CONSTRAINT fk_inspections_assigned_to
                                 FOREIGN KEY (assigned_to) REFERENCES users (id) ON DELETE SET NULL,
                             CONSTRAINT fk_inspections_created_by
                                 FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,

                             CONSTRAINT chk_inspections_type
                                 CHECK (inspection_type IN ('ROUTINE', 'ALERT_FOLLOW_UP', 'WARRANTY_CHECK')),
                             CONSTRAINT chk_inspections_status
                                 CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
                             CONSTRAINT chk_inspections_result
                                 CHECK (result IS NULL OR result IN ('PASSED', 'FAILED', 'NEEDS_FOLLOW_UP')),

    -- A completed inspection must say when it finished and what the outcome was
                             CONSTRAINT chk_inspections_completed
                                 CHECK (status <> 'COMPLETED' OR (result IS NOT NULL AND completed_at IS NOT NULL))
);

-- "Inspection history of this equipment, newest first"
CREATE INDEX idx_inspections_equipment_scheduled ON inspections (equipment_id, scheduled_at DESC);

-- "What is scheduled / in progress, soonest first"
CREATE INDEX idx_inspections_status_scheduled ON inspections (status, scheduled_at);

-- "My assigned inspections"
CREATE INDEX idx_inspections_assigned_to ON inspections (assigned_to, scheduled_at);