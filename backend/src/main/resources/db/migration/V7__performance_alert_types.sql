-- V7: alert types raised by the AI service's performance anomaly detection
ALTER TABLE alerts DROP CONSTRAINT chk_alerts_type;

ALTER TABLE alerts ADD CONSTRAINT chk_alerts_type
    CHECK (alert_type IN ('OVER_TEMPERATURE', 'ZERO_POWER', 'VOLTAGE_OUT_OF_RANGE',
                          'LOW_PERFORMANCE', 'UNUSUAL_DROP', 'SUSTAINED_UNDERPERFORMANCE'));
