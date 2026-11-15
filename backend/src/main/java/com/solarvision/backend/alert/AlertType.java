package com.solarvision.backend.alert;

public enum AlertType {
    // rule-based, checked on every sensor reading
    OVER_TEMPERATURE,
    ZERO_POWER,
    VOLTAGE_OUT_OF_RANGE,

    // found by the AI service (expected vs actual energy), checked once a day
    LOW_PERFORMANCE,
    UNUSUAL_DROP,
    SUSTAINED_UNDERPERFORMANCE
}
