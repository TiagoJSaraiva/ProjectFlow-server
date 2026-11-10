package com.polo42.project_flow.controller;

import com.polo42.project_flow.dto.ProjectDiagramDTO;
import com.polo42.project_flow.dto.SaveProjectDiagramRequest;
import com.polo42.project_flow.security.CustomUserDetails;
import com.polo42.project_flow.service.DiagramService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/diagram")
public class DiagramController {

    @Autowired
    private DiagramService diagramService;

    private static final Logger log = LoggerFactory.getLogger(DiagramController.class);

    @GetMapping
    public ResponseEntity<ProjectDiagramDTO> loadDiagram(@PathVariable UUID projectId,
                                                         @AuthenticationPrincipal CustomUserDetails principal) {
        log.debug("[DiagramController] GET diagram for project {}", projectId);
        return ResponseEntity.ok(diagramService.loadDiagram(projectId, principal.getId()));
    }

    @PutMapping
    public ResponseEntity<ProjectDiagramDTO> saveDiagram(@PathVariable UUID projectId,
                                                         @AuthenticationPrincipal CustomUserDetails principal,
                                                         @RequestBody SaveProjectDiagramRequest request) {
        log.debug("[DiagramController] PUT diagram for project {}", projectId);
        return ResponseEntity.ok(diagramService.saveDiagram(projectId, principal.getId(), request));
    }
}
