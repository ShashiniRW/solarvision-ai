package com.solarvision.backend.project;

import com.solarvision.backend.project.dto.AssignmentRequest;
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
    public ResponseEntity<ProjectAssignment> assignUser(@Valid @RequestBody AssignmentRequest request) {
        ProjectAssignment savedAssignment = assignmentService.assignUserToProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedAssignment);
    }

    @GetMapping
    public ResponseEntity<List<ProjectAssignment>> getAssignments(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long userId) {

        if (projectId != null) {
            return ResponseEntity.ok(assignmentService.getAssignmentsByProject(projectId));
        } else if (userId != null) {
            return ResponseEntity.ok(assignmentService.getAssignmentsByUser(userId));
        } else {
            throw new IllegalArgumentException("Either projectId or userId must be provided");
        }
    }
}