package com.solarvision.backend.inspection;

import com.solarvision.backend.alert.Alert;
import com.solarvision.backend.alert.AlertRepository;
import com.solarvision.backend.common.exception.ResourceNotFoundException;
import com.solarvision.backend.equipment.Equipment;
import com.solarvision.backend.equipment.EquipmentRepository;
import com.solarvision.backend.inspection.dto.CompleteInspectionRequest;
import com.solarvision.backend.inspection.dto.CreateInspectionRequest;
import com.solarvision.backend.user.User;
import com.solarvision.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InspectionServiceTest {

    private static final Long EQUIPMENT_ID = 1L;
    private static final Long INSPECTION_ID = 10L;
    private static final Long ALERT_ID = 5L;
    private static final String EMAIL = "technician@example.com";

    @Mock
    private InspectionRepository inspectionRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InspectionService service;

    // ---------- helpers ----------

    private Equipment equipment(Long id) {
        Equipment equipment = new Equipment();
        equipment.setId(id);
        return equipment;
    }

    private CreateInspectionRequest createRequest(InspectionType type) {
        CreateInspectionRequest request = new CreateInspectionRequest();
        request.setEquipmentId(EQUIPMENT_ID);
        request.setInspectionType(type);
        request.setScheduledAt(OffsetDateTime.parse("2026-11-01T09:00:00+05:30"));
        return request;
    }

    private Inspection inspection(InspectionStatus status) {
        Inspection inspection = new Inspection();
        inspection.setId(INSPECTION_ID);
        inspection.setStatus(status);
        return inspection;
    }

    private void givenInspection(Inspection inspection) {
        when(inspectionRepository.findWithEquipmentById(INSPECTION_ID)).thenReturn(Optional.of(inspection));
    }

    private void givenEquipmentExists() {
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment(EQUIPMENT_ID)));
    }

    private void givenSaveReturnsArgument() {
        when(inspectionRepository.save(any(Inspection.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenUser(Long id) {
        User user = new User();
        user.setId(id);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    private CompleteInspectionRequest completeRequest(InspectionResult result, String findings) {
        CompleteInspectionRequest request = new CompleteInspectionRequest();
        request.setResult(result);
        request.setFindings(findings);
        return request;
    }

    // ---------- create ----------

    @Test
    void create_buildsScheduledInspection_withCreator() {
        givenEquipmentExists();
        givenUser(7L);
        givenSaveReturnsArgument();

        Inspection result = service.create(createRequest(InspectionType.ROUTINE), EMAIL);

        assertThat(result.getStatus()).isEqualTo(InspectionStatus.SCHEDULED);
        assertThat(result.getCreatedBy()).isEqualTo(7L);
        assertThat(result.getEquipment().getId()).isEqualTo(EQUIPMENT_ID);
    }

    @Test
    void create_throwsNotFound_whenEquipmentDoesNotExist() {
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(createRequest(InspectionType.ROUTINE), EMAIL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Equipment not found");
        verify(inspectionRepository, never()).save(any(Inspection.class));
    }

    @Test
    void create_rejectsAlertFollowUp_withoutAlert() {
        assertThatThrownBy(() -> service.create(createRequest(InspectionType.ALERT_FOLLOW_UP), EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must reference an alert");
        verify(inspectionRepository, never()).save(any(Inspection.class));
    }

    @Test
    void create_rejects_whenAlertBelongsToOtherEquipment() {
        givenEquipmentExists();
        Alert alert = new Alert();
        alert.setId(ALERT_ID);
        alert.setEquipment(equipment(2L)); // different equipment
        when(alertRepository.findWithEquipmentById(ALERT_ID)).thenReturn(Optional.of(alert));

        CreateInspectionRequest request = createRequest(InspectionType.ALERT_FOLLOW_UP);
        request.setAlertId(ALERT_ID);

        assertThatThrownBy(() -> service.create(request, EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");
        verify(inspectionRepository, never()).save(any(Inspection.class));
    }

    @Test
    void create_throwsNotFound_whenAlertDoesNotExist() {
        givenEquipmentExists();
        when(alertRepository.findWithEquipmentById(ALERT_ID)).thenReturn(Optional.empty());

        CreateInspectionRequest request = createRequest(InspectionType.ALERT_FOLLOW_UP);
        request.setAlertId(ALERT_ID);

        assertThatThrownBy(() -> service.create(request, EMAIL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Alert not found");
    }

    @Test
    void create_throwsNotFound_whenAssignedUserDoesNotExist() {
        givenEquipmentExists();
        when(userRepository.existsById(99L)).thenReturn(false);

        CreateInspectionRequest request = createRequest(InspectionType.ROUTINE);
        request.setAssignedTo(99L);

        assertThatThrownBy(() -> service.create(request, EMAIL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
        verify(inspectionRepository, never()).save(any(Inspection.class));
    }

    // ---------- getById ----------

    @Test
    void getById_throwsNotFound_whenMissing() {
        when(inspectionRepository.findWithEquipmentById(INSPECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(INSPECTION_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Inspection not found");
    }

    // ---------- start ----------

    @Test
    void start_movesToInProgress_andAssignsStarterWhenUnassigned() {
        givenInspection(inspection(InspectionStatus.SCHEDULED));
        givenUser(7L);
        givenSaveReturnsArgument();

        Inspection result = service.start(INSPECTION_ID, EMAIL);

        assertThat(result.getStatus()).isEqualTo(InspectionStatus.IN_PROGRESS);
        assertThat(result.getStartedAt()).isNotNull();
        assertThat(result.getAssignedTo()).isEqualTo(7L);
    }

    @Test
    void start_keepsExistingAssignee() {
        Inspection scheduled = inspection(InspectionStatus.SCHEDULED);
        scheduled.setAssignedTo(3L);
        givenInspection(scheduled);
        givenUser(7L);
        givenSaveReturnsArgument();

        Inspection result = service.start(INSPECTION_ID, EMAIL);

        assertThat(result.getAssignedTo()).isEqualTo(3L);
    }

    @Test
    void start_rejects_whenNotScheduled() {
        givenInspection(inspection(InspectionStatus.IN_PROGRESS));

        assertThatThrownBy(() -> service.start(INSPECTION_ID, EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only a SCHEDULED inspection");
        verify(inspectionRepository, never()).save(any(Inspection.class));
    }

    // ---------- complete ----------

    @Test
    void complete_passed_needsNoFindings() {
        givenInspection(inspection(InspectionStatus.IN_PROGRESS));
        givenSaveReturnsArgument();

        Inspection result = service.complete(INSPECTION_ID, completeRequest(InspectionResult.PASSED, null));

        assertThat(result.getStatus()).isEqualTo(InspectionStatus.COMPLETED);
        assertThat(result.getResult()).isEqualTo(InspectionResult.PASSED);
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getFindings()).isNull();
    }

    @Test
    void complete_failed_requiresFindings() {
        givenInspection(inspection(InspectionStatus.IN_PROGRESS));

        assertThatThrownBy(() ->
                service.complete(INSPECTION_ID, completeRequest(InspectionResult.FAILED, "   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Findings are required");
        verify(inspectionRepository, never()).save(any(Inspection.class));
    }

    @Test
    void complete_failed_withFindings_isSaved() {
        givenInspection(inspection(InspectionStatus.IN_PROGRESS));
        givenSaveReturnsArgument();

        Inspection result = service.complete(INSPECTION_ID,
                completeRequest(InspectionResult.NEEDS_FOLLOW_UP, "  Loose DC connector  "));

        assertThat(result.getResult()).isEqualTo(InspectionResult.NEEDS_FOLLOW_UP);
        assertThat(result.getFindings()).isEqualTo("Loose DC connector");
    }

    @Test
    void complete_rejects_whenNotInProgress() {
        givenInspection(inspection(InspectionStatus.SCHEDULED));

        assertThatThrownBy(() ->
                service.complete(INSPECTION_ID, completeRequest(InspectionResult.PASSED, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only an IN_PROGRESS inspection");
        verify(inspectionRepository, never()).save(any(Inspection.class));
    }

    // ---------- cancel ----------

    @Test
    void cancel_scheduledInspection_keepsTheReason() {
        givenInspection(inspection(InspectionStatus.SCHEDULED));
        givenSaveReturnsArgument();

        Inspection result = service.cancel(INSPECTION_ID, "Customer rescheduled");

        assertThat(result.getStatus()).isEqualTo(InspectionStatus.CANCELLED);
        assertThat(result.getFindings()).isEqualTo("Customer rescheduled");
    }

    @Test
    void cancel_rejects_whenAlreadyCompleted() {
        givenInspection(inspection(InspectionStatus.COMPLETED));

        assertThatThrownBy(() -> service.cancel(INSPECTION_ID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be cancelled");
        verify(inspectionRepository, never()).save(any(Inspection.class));
    }
}