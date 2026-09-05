package com.solarvision.backend.project;

import com.solarvision.backend.project.dto.AssignmentRequest;
import com.solarvision.backend.project.dto.AssignmentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
public class ProjectAssignmentController {

    private final ProjectAssignmentService assignmentService;

    public ProjectAssignmentController(ProjectAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping
    public ResponseEntity<AssignmentResponse> assignUser(@Valid @RequestBody AssignmentRequest request) {
        ProjectAssignment savedAssignment = assignmentService.assignUserToProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AssignmentResponse.fromEntity(savedAssignment));
    }

    @GetMapping
    public ResponseEntity<List<AssignmentResponse>> getAssignments(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long userId) {

        List<ProjectAssignment> assignments;

        if (projectId != null) {
            assignments = assignmentService.getAssignmentsByProject(projectId);
        } else if (userId != null) {
            assignments = assignmentService.getAssignmentsByUser(userId);
        } else {
            throw new IllegalArgumentException("Either projectId or userId must be provided");
        }

        List<AssignmentResponse> responses = assignments.stream()
                .map(AssignmentResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(responses);
    }
}