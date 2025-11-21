package com.polo42.project_flow.controller;

import com.polo42.project_flow.dto.ProjectDTO;
import com.polo42.project_flow.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<ProjectDTO>> getProjectsByUser(@PathVariable UUID userId) {
        List<ProjectDTO> projects = projectService.findByUsuarioId(userId);
        return ResponseEntity.ok(projects);
    }

    // 🔹 Obter um projeto pelo ID
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDTO> getProjectById(@PathVariable UUID id) {
        ProjectDTO project = projectService.findById(id);
        return ResponseEntity.ok(project);
    }

    // 🔹 Criar um novo projeto vinculado a um usuário
    @PostMapping("/user/{userId}")
    public ResponseEntity<ProjectDTO> createProject(@PathVariable UUID userId, @RequestBody ProjectDTO projectDTO) {
        ProjectDTO savedProject = projectService.create(userId, projectDTO);
        return ResponseEntity.ok(savedProject);
    }

    // 🔹 Atualizar nome ou descrição de um projeto
    @PutMapping("/{id}")
    public ResponseEntity<ProjectDTO> updateProject(@PathVariable UUID id, @RequestBody ProjectDTO projectDTO) {
        ProjectDTO updatedProject = projectService.update(id, projectDTO);
        return ResponseEntity.ok(updatedProject);
    }

    // 🔹 Deletar um projeto
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}