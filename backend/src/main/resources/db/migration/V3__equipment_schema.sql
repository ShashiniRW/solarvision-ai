-- ============================================================
-- SolarVision AI
-- V3: Equipment Schema
-- ============================================================

-- ------------------------------------------------------------
-- Equipment
-- Physical hardware installed at a site: panels, inverters,
-- batteries, sensors, and meters. Each item is uniquely
-- identifiable via a scannable QR code for field technicians.
-- ------------------------------------------------------------
CREATE TABLE equipment (
                           id BIGSERIAL PRIMARY KEY,
                           site_id BIGINT NOT NULL,
                           equipment_type VARCHAR(20) NOT NULL,
                           manufacturer VARCHAR(100),
                           model VARCHAR(100),
                           serial_number VARCHAR(100),
                           qr_code VARCHAR(100) NOT NULL,
                           capacity_kw DECIMAL(8,2),
                           installation_date DATE,
                           warranty_expiry_date DATE,
                           warranty_years INTEGER,
                           status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT fk_equipment_site
                               FOREIGN KEY (site_id) REFERENCES sites(id) ON DELETE CASCADE,

                           CONSTRAINT uq_equipment_qr_code
                               UNIQUE (qr_code),

                           CONSTRAINT chk_equipment_type
                               CHECK (equipment_type IN ('PANEL', 'INVERTER', 'BATTERY', 'SENSOR', 'METER')),

                           CONSTRAINT chk_equipment_status
                               CHECK (status IN ('ACTIVE', 'INACTIVE', 'FAULTY', 'DECOMMISSIONED')),

                           CONSTRAINT chk_equipment_warranty_years
                               CHECK (warranty_years IS NULL OR warranty_years >= 0)
);