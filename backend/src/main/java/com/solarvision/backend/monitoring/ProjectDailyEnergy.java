package com.solarvision.backend.monitoring;

import java.math.BigDecimal;

/** One row of "energy per day for a project": the day (YYYY-MM-DD, Colombo time) and its total. */
public interface ProjectDailyEnergy {

    String getDay();

    BigDecimal getEnergyKwh(); // null when no reading that day has an energy value
}
