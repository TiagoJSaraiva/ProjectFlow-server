package com.polo42.project_flow.controller;

import com.polo42.project_flow.dto.ProjectDTO;
import com.polo42.project_flow.security.CustomUserDetails;
import com.polo42.project_flow.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    // 🔹 Listar todos os projetos de um usuário
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ProjectDTO>> getProjectsByUser(@PathVariable UUID userId,
                                                           @AuthenticationPrincipal CustomUserDetails principal) {
        List<ProjectDTO> projects = projectService.findByUsuarioId(userId, principal.getId());
        return ResponseEntity.ok(projects);
    }

    // 🔹 Obter um projeto pelo ID
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDTO> getProjectById(@PathVariable UUID id,
                                                     @AuthenticationPrincipal CustomUserDetails principal) {
        ProjectDTO project = projectService.findById(id, principal.getId());
        return ResponseEntity.ok(project);
    }

    // 🔹 Criar um novo projeto vinculado a um usuário
    @PostMapping("/user/{userId}")
    public ResponseEntity<ProjectDTO> createProject(@PathVariable UUID userId,
                                                    @AuthenticationPrincipal CustomUserDetails principal,
                                                    @RequestBody ProjectDTO projectDTO) {
        ProjectDTO savedProject = projectService.create(userId, principal.getId(), projectDTO);
        return ResponseEntity.ok(savedProject);
    }

    // 🔹 Atualizar nome ou descrição de um projeto
    @PutMapping("/{id}")
    public ResponseEntity<ProjectDTO> updateProject(@PathVariable UUID id,
                                                    @AuthenticationPrincipal CustomUserDetails principal,
                                                    @RequestBody ProjectDTO projectDTO) {
        ProjectDTO updatedProject = projectService.update(id, principal.getId(), projectDTO);
        return ResponseEntity.ok(updatedProject);
    }

    // 🔹 Deletar um projeto
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        projectService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}