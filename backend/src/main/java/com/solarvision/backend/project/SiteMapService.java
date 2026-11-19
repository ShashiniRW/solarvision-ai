package com.solarvision.backend.project;

import com.solarvision.backend.access.AccessService;
import com.solarvision.backend.access.ProjectScope;
import com.solarvision.backend.alert.AlertRepository;
import com.solarvision.backend.alert.AlertSeverity;
import com.solarvision.backend.alert.AlertStatus;
import com.solarvision.backend.alert.SiteAlertCount;
import com.solarvision.backend.equipment.EquipmentRepository;
import com.solarvision.backend.equipment.SiteEquipmentCount;
import com.solarvision.backend.project.dto.SiteMapItem;
import com.solarvision.backend.project.dto.SiteMapItem.Health;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Data for the site map: every site the current user may see, with its alert status.
 * Three queries in total, however many sites there are (no N+1 problem).
 */
@Service
@RequiredArgsConstructor
public class SiteMapService {

    private final SiteRepository siteRepository;
    private final EquipmentRepository equipmentRepository;
    private final AlertRepository alertRepository;
    private final AccessService accessService;

    @Transactional(readOnly = true)
    public List<SiteMapItem> getMapSites() {
        ProjectScope scope = accessService.currentScope();

        // same access rules as everywhere else in the app
        List<Site> sites = siteRepository.findAllWithProject().stream()
            .filter(site -> scope.allows(site.getProject().getId()))
            .toList();
        if (sites.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> equipmentBySite = equipmentRepository.countBySite().stream()
            .collect(Collectors.toMap(SiteEquipmentCount::siteId, SiteEquipmentCount::count));

        Map<Long, Map<AlertSeverity, Long>> alertsBySite = new HashMap<>();
        for (SiteAlertCount row : alertRepository.countUnresolvedBySiteAndSeverity(AlertStatus.RESOLVED)) {
            alertsBySite.computeIfAbsent(row.siteId(), id -> new EnumMap<>(AlertSeverity.class))
                .put(row.severity(), row.count());
        }

        return sites.stream()
            .map(site -> toItem(site,
                equipmentBySite.getOrDefault(site.getId(), 0L),
                alertsBySite.getOrDefault(site.getId(), Map.of())))
            .toList();
    }

    private SiteMapItem toItem(Site site, long equipmentCount, Map<AlertSeverity, Long> alerts) {
        long critical = alerts.getOrDefault(AlertSeverity.CRITICAL, 0L);
        long warning = alerts.getOrDefault(AlertSeverity.WARNING, 0L);

        Health health = critical > 0 ? Health.CRITICAL
            : warning > 0 ? Health.WARNING
            : Health.OK;

        return new SiteMapItem(
            site.getId(),
            site.getName(),
            site.getProject().getId(),
            site.getProject().getName(),
            site.getLatitude(),
            site.getLongitude(),
            site.getCapacityKw(),
            equipmentCount,
            critical,
            warning,
            health);
    }
}
