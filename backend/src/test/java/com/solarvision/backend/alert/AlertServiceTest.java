package com.solarvision.backend.alert;

import com.solarvision.backend.common.exception.ResourceNotFoundException;
import com.solarvision.backend.user.User;
import com.solarvision.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    private static final Long ALERT_ID = 5L;
    private static final String EMAIL = "engineer@example.com";

    @Mock
    private AlertRuleEvaluator ruleEvaluator;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AlertService service;

    // ---------- helpers ----------

    private Alert alertWithStatus(AlertStatus status) {
        Alert alert = new Alert();
        alert.setId(ALERT_ID);
        alert.setStatus(status);
        return alert;
    }

    private void givenAlert(Alert alert) {
        when(alertRepository.findWithEquipmentById(ALERT_ID)).thenReturn(Optional.of(alert));
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
        givenAlert(alertWithStatus(AlertStatus.OPEN));
        givenUser(7L);
        givenSaveReturnsArgument();

        Alert result = service.acknowledge(ALERT_ID, EMAIL);

        assertThat(result.getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        assertThat(result.getAcknowledgedBy()).isEqualTo(7L);
        assertThat(result.getAcknowledgedAt()).isNotNull();
    }

    @Test
    void acknowledge_alreadyAcknowledgedAlert_isRejected() {
        givenAlert(alertWithStatus(AlertStatus.ACKNOWLEDGED));

        assertThatThrownBy(() -> service.acknowledge(ALERT_ID, EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only an OPEN alert");
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void acknowledge_resolvedAlert_isRejected() {
        givenAlert(alertWithStatus(AlertStatus.RESOLVED));

        assertThatThrownBy(() -> service.acknowledge(ALERT_ID, EMAIL))
                .isInstanceOf(IllegalArgumentException.class);
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
        givenAlert(alertWithStatus(AlertStatus.OPEN));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.acknowledge(ALERT_ID, EMAIL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
        verify(alertRepository, never()).save(any(Alert.class));
    }

    // ---------- resolve ----------

    @Test
    void resolve_acknowledgedAlert_recordsWhoAndWhen() {
        givenAlert(alertWithStatus(AlertStatus.ACKNOWLEDGED));
        givenUser(7L);
        givenSaveReturnsArgument();

        Alert result = service.resolve(ALERT_ID, EMAIL);

        assertThat(result.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(result.getResolvedBy()).isEqualTo(7L);
        assertThat(result.getResolvedAt()).isNotNull();
    }

    @Test
    void resolve_openAlert_isAllowedDirectly() {
        givenAlert(alertWithStatus(AlertStatus.OPEN));
        givenUser(7L);
        givenSaveReturnsArgument();

        Alert result = service.resolve(ALERT_ID, EMAIL);

        assertThat(result.getStatus()).isEqualTo(AlertStatus.RESOLVED);
    }

    @Test
    void resolve_alreadyResolvedAlert_isRejected() {
        givenAlert(alertWithStatus(AlertStatus.RESOLVED));

        assertThatThrownBy(() -> service.resolve(ALERT_ID, EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already resolved");
        verify(alertRepository, never()).save(any(Alert.class));
    }
}