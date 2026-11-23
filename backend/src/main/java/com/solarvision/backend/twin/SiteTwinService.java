package com.solarvision.backend.twin;

import com.solarvision.backend.access.AccessService;
import com.solarvision.backend.alert.AlertRepository;
import com.solarvision.backend.alert.AlertStatus;
import com.solarvision.backend.alert.EquipmentAlertCount;
import com.solarvision.backend.common.exception.ResourceNotFoundException;
import com.solarvision.backend.equipment.Equipment;
import com.solarvision.backend.equipment.EquipmentService;
import com.solarvision.backend.monitoring.SensorReading;
import com.solarvision.backend.monitoring.SensorReadingRepository;
import com.solarvision.backend.project.Site;
import com.solarvision.backend.project.SiteRepository;
import com.solarvision.backend.project.dto.SiteMapItem.Health;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds the data for a site's 3D digital twin: every piece of equipment with
 * its health (worst unresolved alert) and its latest reading.
 * 3 queries in total, however much equipment the site has.
 */
@Service
@RequiredArgsConstructor
public class SiteTwinService {

    private final SiteRepository siteRepository;
    private final EquipmentService equipmentService;
    private final AlertRepository alertRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final AccessService accessService;

    @Transactional(readOnly = true)
    public SiteTwinResponse getTwin(Long siteId) {
        // 404 when the site does not exist OR belongs to a project the user may not see
        Site site = siteRepository.findById(siteId)
            .filter(found -> accessService.canAccessProject(found.getProject().getId()))
            .orElseThrow(() -> new ResourceNotFoundException("Site not found with id: " + siteId));

        List<Equipment> equipment = equipmentService.getEquipmentBySite(siteId);
        Map<Long, Health> healthByEquipment = worstAlertByEquipment(siteId);
        Map<Long, SensorReading> latestByEquipment = sensorReadingRepository.findLatestForSite(siteId).stream()
            .collect(Collectors.toMap(reading -> reading.getEquipment().getId(),
                Function.identity(),
                (first, second) -> first)); // two readings with the same newest time: keep one

        List<TwinEquipment> items = equipment.stream()
            .map(item -> toTwin(item,
                healthByEquipment.getOrDefault(item.getId(), Health.OK),
                latestByEquipment.get(item.getId())))
            .toList();

        return new SiteTwinResponse(site.getId(), site.getName(), items);
    }

    private Map<Long, Health> worstAlertByEquipment(Long siteId) {
        Map<Long, Health> result = new HashMap<>();
        for (EquipmentAlertCount count : alertRepository.countUnresolvedByEquipmentForSite(siteId, AlertStatus.RESOLVED)) {
            Health health = switch (count.severity()) {
                case CRITICAL -> Health.CRITICAL;
                case WARNING -> Health.WARNING;
                default -> Health.OK;
            };
            result.merge(count.equipmentId(), health, SiteTwinService::worse);
        }
        return result;
    }

    private static Health worse(Health first, Health second) {
        return first.ordinal() >= second.ordinal() ? first : second; // OK < WARNING < CRITICAL
    }

    private static TwinEquipment toTwin(Equipment item, Health alertHealth, SensorReading latest) {
        // equipment marked FAULTY is always shown as critical, even without an alert
        Health health = "FAULTY".equals(item.getStatus()) ? Health.CRITICAL : alertHealth;

        return new TwinEquipment(
            item.getId(),
            item.getEquipmentType(),
            item.getSerialNumber(),
            item.getModel(),
            item.getCapacityKw(),
            item.getStatus(),
            health,
            latest != null ? latest.getPowerKw() : null,
            latest != null ? latest.getTemperatureC() : null,
            latest != null ? latest.getRecordedAt() : null);
    }
}
