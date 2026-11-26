package com.solarvision.backend.monitoring;

import java.math.BigDecimal;

/** One number per site, for example "energy today" or "live power" (null when unknown). */
public record SiteValue(Long siteId, BigDecimal value) {
}
