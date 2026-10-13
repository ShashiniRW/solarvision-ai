package com.solarvision.backend.monitoring;

import com.solarvision.backend.equipment.Equipment;
import com.solarvision.backend.equipment.EquipmentRepository;
import com.solarvision.backend.monitoring.dto.CreateSensorReadingRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class SensorReadingService {

    private final SensorReadingRepository sensorReadingRepository;
    private final EquipmentRepository equipmentRepository;

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
}