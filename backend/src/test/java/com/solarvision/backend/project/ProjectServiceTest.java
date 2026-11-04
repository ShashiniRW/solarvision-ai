package com.solarvision.backend.project;

import com.solarvision.backend.access.AccessService;
import com.solarvision.backend.access.ProjectScope;
import com.solarvision.backend.common.exception.ResourceNotFoundException;
import com.solarvision.backend.organization.OrganizationRepository;
import com.solarvision.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccessService accessService;

    @InjectMocks
    private ProjectService service;

    @Test
    void getAllProjects_returnsEverything_forAnAdmin() {
        Project project = new Project();
        when(accessService.currentScope()).thenReturn(ProjectScope.everything());
        when(projectRepository.findAllWithDetails()).thenReturn(List.of(project));

        assertThat(service.getAllProjects()).containsExactly(project);
        verify(projectRepository, never()).findByIdsWithDetails(any());
    }

    @Test
    void getAllProjects_returnsOnlyTheProjectsInScope() {
        Project project = new Project();
        when(accessService.currentScope()).thenReturn(ProjectScope.only(Set.of(1L, 2L)));
        when(projectRepository.findByIdsWithDetails(
                argThat(ids -> ids != null && ids.size() == 2 && ids.containsAll(Set.of(1L, 2L)))))
                .thenReturn(List.of(project));

        assertThat(service.getAllProjects()).containsExactly(project);
        verify(projectRepository, never()).findAllWithDetails();
    }

    @Test
    void getAllProjects_returnsNothing_whenTheUserMaySeeNoProject() {
        when(accessService.currentScope()).thenReturn(ProjectScope.only(Set.of()));

        assertThat(service.getAllProjects()).isEmpty();
        verifyNoInteractions(projectRepository);
    }

    @Test
    void getProjectById_checksAccessBeforeLoadingAnything() {
        doThrow(new ResourceNotFoundException("Project not found"))
                .when(accessService).requireProjectAccess(5L);

        assertThatThrownBy(() -> service.getProjectById(5L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found");
        verify(projectRepository, never()).findByIdWithDetails(anyLong());
    }

    @Test
    void getProjectById_returnsTheProject_whenTheUserMaySeeIt() {
        Project project = new Project();
        when(projectRepository.findByIdWithDetails(5L)).thenReturn(Optional.of(project));

        assertThat(service.getProjectById(5L)).isSameAs(project);
        verify(accessService).requireProjectAccess(5L);
    }
}