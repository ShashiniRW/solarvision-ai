package com.solarvision.backend.alert;

/** Number of unresolved alerts of one severity at one site (a row of a GROUP BY query). */
public record SiteAlertCount(Long siteId, AlertSeverity severity, Long count) {
}
