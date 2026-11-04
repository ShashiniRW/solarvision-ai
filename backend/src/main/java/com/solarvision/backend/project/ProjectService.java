package com.solarvision.backend.project;

import com.solarvision.backend.access.AccessService;
import com.solarvision.backend.access.ProjectScope;
import com.solarvision.backend.common.exception.ResourceNotFoundException;
import com.solarvision.backend.organization.Organization;
import com.solarvision.backend.organization.OrganizationRepository;
import com.solarvision.backend.project.dto.ProjectRequest;
import com.solarvision.backend.user.User;
import com.solarvision.backend.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final AccessService accessService;

    public ProjectService(ProjectRepository projectRepository,
                          OrganizationRepository organizationRepository,
                          UserRepository userRepository,
                          AccessService accessService) {
        this.projectRepository = projectRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
    }

    public Project createProject(ProjectRequest request) {

        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        Project project = new Project();
        project.setOrganization(organization);
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setStatus("PLANNED");

        if (request.getCustomerId() != null) {
            User customer = userRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
            project.setCustomer(customer);
        }

        return projectRepository.save(project);
    }

    /** Only the projects the current user may see. */
    public List<Project> getAllProjects() {
        ProjectScope scope = accessService.currentScope();

        if (scope.unrestricted()) {
            return projectRepository.findAllWithDetails();
        }
        if (scope.projectIds().isEmpty()) {
            return List.of();
        }
        return projectRepository.findByIdsWithDetails(scope.projectIds());
    }

    /** A project the current user may not see looks exactly like one that does not exist. */
    public Project getProjectById(Long id) {
        accessService.requireProjectAccess(id);

        return projectRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

}