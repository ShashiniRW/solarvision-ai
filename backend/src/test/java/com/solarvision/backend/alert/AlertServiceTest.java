package com.solarvision.backend.alert;

import com.solarvision.backend.access.AccessService;
import com.solarvision.backend.access.ProjectScope;
import com.solarvision.backend.common.exception.ConflictException;
import com.solarvision.backend.common.exception.ResourceNotFoundException;
import com.solarvision.backend.equipment.Equipment;
import com.solarvision.backend.project.Project;
import com.solarvision.backend.project.Site;
import com.solarvision.backend.user.User;
import com.solarvision.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    private static final Long ALERT_ID = 5L;
    private static final Long PROJECT_ID = 1L;
    private static final Long EQUIPMENT_ID = 10L;
    private static final String EMAIL = "engineer@example.com";

    @Mock
    private AlertRuleEvaluator ruleEvaluator;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccessService accessService;

    @InjectMocks
    private AlertService service;

    // ---------- helpers ----------

    /** An alert on equipment 10, at a site of project 1. */
    private Alert alertWithStatus(AlertStatus status) {
        Project project = new Project();
        project.setId(PROJECT_ID);
        Site site = new Site();
        site.setProject(project);
        Equipment equipment = new Equipment();
        equipment.setId(EQUIPMENT_ID);
        equipment.setSite(site);

        Alert alert = new Alert();
        alert.setId(ALERT_ID);
        alert.setStatus(status);
        alert.setEquipment(equipment);
        return alert;
    }

    private void givenAlert(Alert alert) {
        when(alertRepository.findWithEquipmentById(ALERT_ID)).thenReturn(Optional.of(alert));
    }

    private void givenVisibleAlert(AlertStatus status) {
        givenAlert(alertWithStatus(status));
        when(accessService.canAccessProject(PROJECT_ID)).thenReturn(true);
    }

    private void givenHiddenAlert(AlertStatus status) {
        givenAlert(alertWithStatus(status));
        when(accessService.canAccessProject(PROJECT_ID)).thenReturn(false);
    }

    private void givenSaveReturnsArgument() {
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenUser(Long id) {
        User user = new User();
        user.setId(id);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    // ---------- acknowledge ----------

    @Test
    void acknowledge_openAlert_recordsWhoAndWhen() {
        givenVisibleAlert(AlertStatus.OPEN);
        givenUser(7L);
        givenSaveReturnsArgument();

        Alert result = service.acknowledge(ALERT_ID, EMAIL);

        assertThat(result.getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        assertThat(result.getAcknowledgedBy()).isEqualTo(7L);
        assertThat(result.getAcknowledgedAt()).isNotNull();
    }

    @Test
    void acknowledge_alreadyAcknowledgedAlert_isRejected() {
        givenVisibleAlert(AlertStatus.ACKNOWLEDGED);

        assertThatThrownBy(() -> service.acknowledge(ALERT_ID, EMAIL))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Only an OPEN alert");
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void acknowledge_resolvedAlert_isRejected() {
        givenVisibleAlert(AlertStatus.RESOLVED);

        assertThatThrownBy(() -> service.acknowledge(ALERT_ID, EMAIL))
                .isInstanceOf(ConflictException.class);
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void acknowledge_unknownAlert_throwsNotFound() {
        when(alertRepository.findWithEquipmentById(ALERT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.acknowledge(ALERT_ID, EMAIL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Alert not found");
    }

    @Test
    void acknowledge_unknownUser_throwsNotFound() {
        givenVisibleAlert(AlertStatus.OPEN);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.acknowledge(ALERT_ID, EMAIL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void acknowledge_alertInHiddenProject_throws404_andSavesNothing() {
        givenHiddenAlert(AlertStatus.OPEN);

        assertThatThrownBy(() -> service.acknowledge(ALERT_ID, EMAIL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Alert not found");
        verify(alertRepository, never()).save(any(Alert.class));
    }

    // ---------- resolve ----------

    @Test
    void resolve_acknowledgedAlert_recordsWhoAndWhen() {
        givenVisibleAlert(AlertStatus.ACKNOWLEDGED);
        givenUser(7L);
        givenSaveReturnsArgument();

        Alert result = service.resolve(ALERT_ID, EMAIL);

        assertThat(result.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(result.getResolvedBy()).isEqualTo(7L);
        assertThat(result.getResolvedAt()).isNotNull();
    }

    @Test
    void resolve_openAlert_isAllowedDirectly() {
        givenVisibleAlert(AlertStatus.OPEN);
        givenUser(7L);
        givenSaveReturnsArgument();

        Alert result = service.resolve(ALERT_ID, EMAIL);

        assertThat(result.getStatus()).isEqualTo(AlertStatus.RESOLVED);
    }

    @Test
    void resolve_alreadyResolvedAlert_isRejected() {
        givenVisibleAlert(AlertStatus.RESOLVED);

        assertThatThrownBy(() -> service.resolve(ALERT_ID, EMAIL))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already resolved");
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void resolve_alertInHiddenProject_throws404_andSavesNothing() {
        givenHiddenAlert(AlertStatus.OPEN);

        assertThatThrownBy(() -> service.resolve(ALERT_ID, EMAIL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Alert not found");
        verify(alertRepository, never()).save(any(Alert.class));
    }

    // ---------- getById ----------

    @Test
    void getById_returnsAlert_whenProjectIsVisible() {
        givenVisibleAlert(AlertStatus.OPEN);

        assertThat(service.getById(ALERT_ID).getId()).isEqualTo(ALERT_ID);
    }

    @Test
    void getById_throws404_whenProjectIsHidden() {
        givenHiddenAlert(AlertStatus.OPEN);

        assertThatThrownBy(() -> service.getById(ALERT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Alert not found");
    }

    // ---------- search ----------

    @Test
    void search_admin_usesTheUnscopedQuery() {
        when(accessService.currentScope()).thenReturn(ProjectScope.everything());
        List<Alert> all = List.of(alertWithStatus(AlertStatus.OPEN));
        when(alertRepository.findAllByOrderByTriggeredAtDesc(any(Pageable.class))).thenReturn(all);

        assertThat(service.search(null, null, 0, 20)).isSameAs(all);
    }

    @Test
    void search_normalUser_isFilteredByTheirProjects() {
        when(accessService.currentScope()).thenReturn(ProjectScope.only(Set.of(1L, 2L)));
        List<Alert> mine = List.of(alertWithStatus(AlertStatus.OPEN));
        when(alertRepository.findInProjects(eq(Set.of(1L, 2L)), any(Pageable.class))).thenReturn(mine);

        assertThat(service.search(null, null, 0, 20)).isSameAs(mine);
        verify(alertRepository, never()).findAllByOrderByTriggeredAtDesc(any(Pageable.class));
    }

    @Test
    void search_normalUserWithStatus_isFilteredByTheirProjects() {
        when(accessService.currentScope()).thenReturn(ProjectScope.only(Set.of(1L)));
        List<Alert> mine = List.of(alertWithStatus(AlertStatus.OPEN));
        when(alertRepository.findByStatusInProjects(eq(AlertStatus.OPEN), eq(Set.of(1L)), any(Pageable.class)))
                .thenReturn(mine);

        assertThat(service.search(AlertStatus.OPEN, null, 0, 20)).isSameAs(mine);
    }

    @Test
    void search_userWithNoProjects_getsEmptyList_withoutTouchingTheDatabase() {
        when(accessService.currentScope()).thenReturn(ProjectScope.only(Set.of()));

        assertThat(service.search(null, null, 0, 20)).isEmpty();
        verifyNoInteractions(alertRepository);
    }

    @Test
    void search_byHiddenEquipment_throws404() {
        doThrow(new ResourceNotFoundException("Equipment not found"))
                .when(accessService).requireEquipmentAccess(EQUIPMENT_ID);

        assertThatThrownBy(() -> service.search(null, EQUIPMENT_ID, 0, 20))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Equipment not found");
        verifyNoInteractions(alertRepository);
    }

    @Test
    void search_pageSize_isCappedAt100() {
        when(accessService.currentScope()).thenReturn(ProjectScope.everything());

        service.search(null, null, 0, 500);

        verify(alertRepository).findAllByOrderByTriggeredAtDesc(PageRequest.of(0, 100));
    }
}