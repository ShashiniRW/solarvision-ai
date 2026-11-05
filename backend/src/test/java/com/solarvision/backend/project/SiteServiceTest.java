package com.solarvision.backend.project;

import com.solarvision.backend.access.AccessService;
import com.solarvision.backend.common.exception.ResourceNotFoundException;
import com.solarvision.backend.project.dto.SiteRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteServiceTest {

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private AccessService accessService;

    @InjectMocks
    private SiteService service;

    private Site siteOfProject(Long projectId) {
        Project project = new Project();
        project.setId(projectId);
        Site site = new Site();
        site.setProject(project);
        return site;
    }

    // ---------- getSitesByProject ----------

    @Test
    void getSitesByProject_checksProjectAccessBeforeLoadingSites() {
        doThrow(new ResourceNotFoundException("Project not found"))
                .when(accessService).requireProjectAccess(5L);

        assertThatThrownBy(() -> service.getSitesByProject(5L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found");
        verify(siteRepository, never()).findByProjectIdWithDetails(anyLong());
    }

    @Test
    void getSitesByProject_returnsTheSites_whenTheUserMaySeeTheProject() {
        Site site = siteOfProject(5L);
        when(siteRepository.findByProjectIdWithDetails(5L)).thenReturn(List.of(site));

        assertThat(service.getSitesByProject(5L)).containsExactly(site);
        verify(accessService).requireProjectAccess(5L);
    }

    // ---------- getSiteById ----------

    @Test
    void getSiteById_throwsNotFound_whenTheSiteDoesNotExist() {
        when(siteRepository.findByIdWithDetails(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSiteById(9L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Site not found");
    }

    @Test
    void getSiteById_hidesASiteOfAProjectTheUserMayNotSee() {
        when(siteRepository.findByIdWithDetails(9L)).thenReturn(Optional.of(siteOfProject(7L)));
        when(accessService.canAccessProject(7L)).thenReturn(false);

        assertThatThrownBy(() -> service.getSiteById(9L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Site not found");
    }

    @Test
    void getSiteById_returnsTheSite_whenTheUserMaySeeItsProject() {
        Site site = siteOfProject(7L);
        when(siteRepository.findByIdWithDetails(9L)).thenReturn(Optional.of(site));
        when(accessService.canAccessProject(7L)).thenReturn(true);

        assertThat(service.getSiteById(9L)).isSameAs(site);
    }

    // ---------- createSite ----------

    @Test
    void createSite_requiresAccessToTheProject() {
        SiteRequest request = mock(SiteRequest.class);
        when(request.getProjectId()).thenReturn(5L);
        doThrow(new ResourceNotFoundException("Project not found"))
                .when(accessService).requireProjectAccess(5L);

        assertThatThrownBy(() -> service.createSite(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found");
        verify(projectRepository, never()).findById(anyLong());
        verify(siteRepository, never()).save(any(Site.class));
    }
}