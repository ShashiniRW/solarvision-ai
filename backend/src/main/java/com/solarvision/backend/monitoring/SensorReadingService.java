package com.solarvision.backend.monitoring;

import com.solarvision.backend.equipment.Equipment;
import com.solarvision.backend.equipment.EquipmentRepository;
import com.solarvision.backend.monitoring.dto.CreateSensorReadingRequest;
import com.solarvision.backend.monitoring.dto.DailyEnergySummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SensorReadingService {

    private final SensorReadingRepository sensorReadingRepository;
    private final EquipmentRepository equipmentRepository;

    // For now every site uses one time zone; later this will come from the site
    private static final ZoneId SITE_ZONE = ZoneId.of("Asia/Colombo");

    @Transactional
    public SensorReading create(CreateSensorReadingRequest request) {

        Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Equipment not found with id: " + request.getEquipmentId()));

        // a reading with no measurements at all is meaningless
        if (request.getPowerKw() == null && request.getEnergyKwh() == null
                && request.getVoltageV() == null && request.getCurrentA() == null
                && request.getTemperatureC() == null) {
            throw new IllegalArgumentException(
                    "At least one measurement (power, energy, voltage, current or temperature) is required");
        }

        OffsetDateTime recordedAt = request.getRecordedAt() != null
                ? request.getRecordedAt()
                : OffsetDateTime.now();

        if (sensorReadingRepository.existsByEquipmentIdAndRecordedAt(equipment.getId(), recordedAt)) {
            throw new IllegalArgumentException(
                    "A reading already exists for this equipment at " + recordedAt);
        }

        SensorReading reading = new SensorReading();
        reading.setEquipment(equipment);
        reading.setRecordedAt(recordedAt);
        reading.setPowerKw(request.getPowerKw());
        reading.setEnergyKwh(request.getEnergyKwh());
        reading.setVoltageV(request.getVoltageV());
        reading.setCurrentA(request.getCurrentA());
        reading.setTemperatureC(request.getTemperatureC());
        reading.setIsSimulated(request.getIsSimulated() != null ? request.getIsSimulated() : true);

        return sensorReadingRepository.save(reading);
    }

    @Transactional(readOnly = true)
    public SensorReading getLatest(Long equipmentId) {
        if (!equipmentRepository.existsById(equipmentId)) {
            throw new IllegalArgumentException("Equipment not found with id: " + equipmentId);
        }
        return sensorReadingRepository.findFirstByEquipmentIdOrderByRecordedAtDesc(equipmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No readings found for equipment id: " + equipmentId));
    }

    @Transactional(readOnly = true)
    public List<SensorReading> getRange(Long equipmentId, OffsetDateTime from, OffsetDateTime to) {
        if (!equipmentRepository.existsById(equipmentId)) {
            throw new IllegalArgumentException("Equipment not found with id: " + equipmentId);
        }
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("'from' must be earlier than 'to'");
        }
        if (from.plusDays(31).isBefore(to)) {
            throw new IllegalArgumentException("Time range cannot exceed 31 days");
        }
        return sensorReadingRepository
                .findByEquipmentIdAndRecordedAtBetweenOrderByRecordedAtAsc(equipmentId, from, to);
    }

    @Transactional(readOnly = true)
    public List<DailyEnergySummaryResponse> getDailySummary(Long equipmentId, LocalDate from, LocalDate to) {
        if (!equipmentRepository.existsById(equipmentId)) {
            throw new IllegalArgumentException("Equipment not found with id: " + equipmentId);
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must not be later than 'to'");
        }
        if (ChronoUnit.DAYS.between(from, to) > 92) {
            throw new IllegalArgumentException("Date range cannot exceed 92 days");
        }

        // from the start of the first local day up to (not including) the start of the day after 'to'
        OffsetDateTime fromTs = from.atStartOfDay(SITE_ZONE).toOffsetDateTime();
        OffsetDateTime toTs = to.plusDays(1).atStartOfDay(SITE_ZONE).toOffsetDateTime();

        return sensorReadingRepository
                .findDailySummary(equipmentId, fromTs, toTs, SITE_ZONE.getId())
                .stream()
                .map(DailyEnergySummaryResponse::fromProjection)
                .toList();
    }
}