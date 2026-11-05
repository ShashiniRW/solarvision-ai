package com.solarvision.backend.project;

import com.solarvision.backend.access.AccessService;
import com.solarvision.backend.common.exception.ResourceNotFoundException;
import com.solarvision.backend.project.dto.SiteRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SiteService {

    private final SiteRepository siteRepository;
    private final ProjectRepository projectRepository;
    private final AccessService accessService;

    public SiteService(SiteRepository siteRepository,
                       ProjectRepository projectRepository,
                       AccessService accessService) {
        this.siteRepository = siteRepository;
        this.projectRepository = projectRepository;
        this.accessService = accessService;
    }

    public Site createSite(SiteRequest request) {

        // a site can only be added to a project the user may see
        accessService.requireProjectAccess(request.getProjectId());

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Site site = new Site();
        site.setProject(project);
        site.setName(request.getName());
        site.setAddress(request.getAddress());
        site.setLatitude(request.getLatitude());
        site.setLongitude(request.getLongitude());
        site.setCapacityKw(request.getCapacityKw());
        site.setStatus("ACTIVE");

        Site savedSite = siteRepository.save(site);
        return siteRepository.findByIdWithDetails(savedSite.getId()).orElseThrow();
    }

    public List<Site> getSitesByProject(Long projectId) {
        accessService.requireProjectAccess(projectId);
        return siteRepository.findByProjectIdWithDetails(projectId);
    }

    public Site getSiteById(Long id) {
        Site site = siteRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Site not found"));

        // a site of a project the user may not see looks exactly like a site that does not exist
        if (!accessService.canAccessProject(site.getProject().getId())) {
            throw new ResourceNotFoundException("Site not found");
        }
        return site;
    }
}