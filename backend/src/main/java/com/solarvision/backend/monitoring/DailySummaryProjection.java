package com.solarvision.backend.monitoring;

import java.math.BigDecimal;

public interface DailySummaryProjection {

    String getDay();

    BigDecimal getTotalEnergyKwh();

    BigDecimal getPeakPowerKw();

    BigDecimal getAvgTemperatureC();

    Long getReadingCount();
}