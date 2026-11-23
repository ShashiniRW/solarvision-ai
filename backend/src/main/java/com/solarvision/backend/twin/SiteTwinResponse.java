package com.solarvision.backend.twin;

import java.util.List;

public record SiteTwinResponse(Long siteId, String siteName, List<TwinEquipment> equipment) {
}
