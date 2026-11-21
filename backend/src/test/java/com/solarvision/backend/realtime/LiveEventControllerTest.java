package com.solarvision.backend.realtime;

import com.solarvision.backend.access.AccessService;
import com.solarvision.backend.access.ProjectScope;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

class LiveEventControllerTest {

    private final LiveEventRegistry registry = new LiveEventRegistry();
    private final AccessService accessService = mock(AccessService.class);
    private final MockMvc mvc = MockMvcBuilders
        .standaloneSetup(new LiveEventController(registry, accessService))
        .build();

    private MvcResult connect(ProjectScope scope) throws Exception {
        when(accessService.currentScope()).thenReturn(scope);
        return mvc.perform(get("/api/events"))
            .andExpect(request().asyncStarted())   // the connection stays open
            .andReturn();
    }

    private static LiveEvent criticalAlert(long projectId, String message) {
        return new LiveEvent(LiveEventType.ALERT_CREATED, projectId, "CRITICAL", message);
    }

    @Test
    void userReceivesOnlyEventsFromTheirOwnProjects() throws Exception {
        MvcResult result = connect(ProjectScope.only(Set.of(1L)));

        registry.onLiveEvent(criticalAlert(1L, "Inverter overheating"));
        registry.onLiveEvent(criticalAlert(2L, "Secret other project"));

        assertThat(result.getResponse().getContentAsString())
            .contains("Inverter overheating")
            .doesNotContain("Secret other project");
    }

    @Test
    void adminReceivesEventsFromAllProjects() throws Exception {
        MvcResult result = connect(ProjectScope.everything());

        registry.onLiveEvent(criticalAlert(1L, "First project"));
        registry.onLiveEvent(criticalAlert(2L, "Second project"));

        assertThat(result.getResponse().getContentAsString())
            .contains("First project")
            .contains("Second project");
    }

    @Test
    void userWithoutProjectsReceivesNothing() throws Exception {
        MvcResult result = connect(ProjectScope.only(Set.of()));

        registry.onLiveEvent(criticalAlert(1L, "Not for you"));

        assertThat(result.getResponse().getContentAsString()).doesNotContain("Not for you");
    }

    @Test
    void heartbeatKeepsTheConnectionAlive() throws Exception {
        MvcResult result = connect(ProjectScope.everything());

        registry.sendHeartbeat();

        assertThat(result.getResponse().getContentAsString()).contains(":ping");
        assertThat(registry.connectedClients()).isEqualTo(1);
    }
}
