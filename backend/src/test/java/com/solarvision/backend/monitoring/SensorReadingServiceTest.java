package com.solarvision.backend.monitoring;

import com.solarvision.backend.alert.AlertService;
import com.solarvision.backend.common.exception.ResourceNotFoundException;
import com.solarvision.backend.equipment.Equipment;
import com.solarvision.backend.equipment.EquipmentRepository;
import com.solarvision.backend.monitoring.dto.CreateSensorReadingRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorReadingServiceTest {

    private static final Long EQUIPMENT_ID = 1L;
    private static final OffsetDateTime RECORDED_AT = OffsetDateTime.parse("2026-10-09T10:00:00+05:30");

    @Mock
    private SensorReadingRepository sensorReadingRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private AlertService alertService;

    @InjectMocks
    private SensorReadingService service;

    // ---------- helpers ----------

    private Equipment equipment() {
        Equipment equipment = new Equipment();
        equipment.setId(EQUIPMENT_ID);
        return equipment;
    }

    private CreateSensorReadingRequest requestWithPower(BigDecimal powerKw) {
        CreateSensorReadingRequest request = new CreateSensorReadingRequest();
        request.setEquipmentId(EQUIPMENT_ID);
        request.setRecordedAt(RECORDED_AT);
        request.setPowerKw(powerKw);
        return request;
    }

    // ---------- create ----------

    @Test
    void create_savesReading_andDefaultsToSimulated() {
        Equipment equipment = equipment();
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment));
        when(sensorReadingRepository.save(any(SensorReading.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SensorReading result = service.create(requestWithPower(new BigDecimal("4.250")));

        assertThat(result.getEquipment()).isSameAs(equipment);
        assertThat(result.getRecordedAt()).isEqualTo(RECORDED_AT);
        assertThat(result.getPowerKw()).isEqualByComparingTo("4.250");
        assertThat(result.getIsSimulated()).isTrue();
        verify(sensorReadingRepository).save(any(SensorReading.class));
    }

    @Test
    void create_evaluatesAlertRules_forTheSavedReading() {
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment()));
        when(sensorReadingRepository.save(any(SensorReading.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SensorReading saved = service.create(requestWithPower(new BigDecimal("4.250")));

        verify(alertService).evaluate(saved);
    }

    @Test
    void create_throwsNotFound_whenEquipmentDoesNotExist() {
        when(equipmentRepository.findById(99999L)).thenReturn(Optional.empty());

        CreateSensorReadingRequest request = requestWithPower(new BigDecimal("4.250"));
        request.setEquipmentId(99999L);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99999");
        verify(sensorReadingRepository, never()).save(any(SensorReading.class));
    }

    @Test
    void create_rejectsReading_withNoMeasurements() {
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment()));

        CreateSensorReadingRequest request = requestWithPower(null); // no measurements at all

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one measurement");
        verify(sensorReadingRepository, never()).save(any(SensorReading.class));
    }

    @Test
    void create_rejectsDuplicateReading_forSameEquipmentAndTime() {
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment()));
        when(sensorReadingRepository.existsByEquipmentIdAndRecordedAt(EQUIPMENT_ID, RECORDED_AT))
                .thenReturn(true);

        CreateSensorReadingRequest request = requestWithPower(new BigDecimal("4.250"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
        verify(sensorReadingRepository, never()).save(any(SensorReading.class));
    }

    // ---------- getLatest ----------

    @Test
    void getLatest_throwsNotFound_whenEquipmentHasNoReadings() {
        when(equipmentRepository.existsById(EQUIPMENT_ID)).thenReturn(true);
        when(sensorReadingRepository.findFirstByEquipmentIdOrderByRecordedAtDesc(EQUIPMENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLatest(EQUIPMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No readings found");
    }

    // ---------- getRange ----------

    @Test
    void getRange_throwsNotFound_whenEquipmentDoesNotExist() {
        when(equipmentRepository.existsById(EQUIPMENT_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.getRange(EQUIPMENT_ID, RECORDED_AT, RECORDED_AT.plusHours(1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getRange_rejects_whenFromIsNotBeforeTo() {
        when(equipmentRepository.existsById(EQUIPMENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.getRange(EQUIPMENT_ID, RECORDED_AT.plusHours(1), RECORDED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'from' must be earlier than 'to'");
    }

    @Test
    void getRange_rejects_whenRangeIsJustOver31Days() {
        when(equipmentRepository.existsById(EQUIPMENT_ID)).thenReturn(true);

        OffsetDateTime from = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        OffsetDateTime to = from.plusDays(31).plusSeconds(1);

        assertThatThrownBy(() -> service.getRange(EQUIPMENT_ID, from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("31 days");
    }

    @Test
    void getRange_allows_rangeOfExactly31Days() {
        when(equipmentRepository.existsById(EQUIPMENT_ID)).thenReturn(true);

        OffsetDateTime from = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        OffsetDateTime to = from.plusDays(31);
        when(sensorReadingRepository
                .findByEquipmentIdAndRecordedAtBetweenOrderByRecordedAtAsc(EQUIPMENT_ID, from, to))
                .thenReturn(List.of());

        assertThat(service.getRange(EQUIPMENT_ID, from, to)).isEmpty();
    }

    // ---------- getDailySummary ----------

    @Test
    void getDailySummary_rejects_whenRangeIsLongerThan92Days() {
        when(equipmentRepository.existsById(EQUIPMENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.getDailySummary(
                EQUIPMENT_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 10, 14)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("92 days");
    }
}