package com.solarvision.backend.project;

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

    public ProjectService(ProjectRepository projectRepository,
                          OrganizationRepository organizationRepository,
                          UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
    }

    public Project createProject(ProjectRequest request) {

        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        Project project = new Project();
        project.setOrganization(organization);
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setStatus("PLANNED");

        if (request.getCustomerId() != null) {
            User customer = userRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
            project.setCustomer(customer);
        }

        return projectRepository.save(project);
    }

    public List<Project> getAllProjects() {
        return projectRepository.findAllWithDetails();
    }

    public Project getProjectById(Long id) {
        return projectRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
    }

}