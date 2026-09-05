package com.solarvision.backend.project;

import com.solarvision.backend.project.dto.AssignmentRequest;
import com.solarvision.backend.user.User;
import com.solarvision.backend.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class ProjectAssignmentService {

    private static final Set<String> VALID_ROLES = Set.of("MANAGER", "ENGINEER", "TECHNICIAN", "WORKER");

    private final ProjectAssignmentRepository assignmentRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectAssignmentService(ProjectAssignmentRepository assignmentRepository,
                                    ProjectRepository projectRepository,
                                    UserRepository userRepository) {
        this.assignmentRepository = assignmentRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    public ProjectAssignment assignUserToProject(AssignmentRequest request) {

        if (!VALID_ROLES.contains(request.getRoleInProject())) {
            throw new IllegalArgumentException(
                    "Invalid role. Must be one of: " + VALID_ROLES);
        }

        if (assignmentRepository.existsByProjectIdAndUserId(request.getProjectId(), request.getUserId())) {
            throw new IllegalArgumentException("User is already assigned to this project");
        }

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ProjectAssignment assignment = new ProjectAssignment();
        assignment.setProject(project);
        assignment.setUser(user);
        assignment.setRoleInProject(request.getRoleInProject());

        ProjectAssignment saved = assignmentRepository.save(assignment);
        return assignmentRepository.findByIdWithDetails(saved.getId()).orElseThrow();
    }

    public List<ProjectAssignment> getAssignmentsByProject(Long projectId) {
        return assignmentRepository.findByProjectIdWithDetails(projectId);
    }

    public List<ProjectAssignment> getAssignmentsByUser(Long userId) {
        return assignmentRepository.findByUserIdWithDetails(userId);
    }
}