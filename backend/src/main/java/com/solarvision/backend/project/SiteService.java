package com.solarvision.backend.project;

import com.solarvision.backend.project.dto.SiteRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SiteService {

    private final SiteRepository siteRepository;
    private final ProjectRepository projectRepository;

    public SiteService(SiteRepository siteRepository, ProjectRepository projectRepository) {
        this.siteRepository = siteRepository;
        this.projectRepository = projectRepository;
    }

    public Site createSite(SiteRequest request) {

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

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
        return siteRepository.findByProjectIdWithDetails(projectId);
    }

    public Site getSiteById(Long id) {
        return siteRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Site not found"));
    }
}