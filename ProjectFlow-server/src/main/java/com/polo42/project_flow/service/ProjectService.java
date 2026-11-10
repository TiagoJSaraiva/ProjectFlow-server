package com.polo42.project_flow.service;

import com.polo42.project_flow.dto.ProjectDTO;
import com.polo42.project_flow.exception.ResourceNotFoundException;
import com.polo42.project_flow.model.Project;
import com.polo42.project_flow.model.User;
import com.polo42.project_flow.repository.ProjectRepository;
import com.polo42.project_flow.repository.DiagramNodeRepository;
import com.polo42.project_flow.repository.DiagramConnectionRepository;
import com.polo42.project_flow.repository.DiagramViewStateRepository;
import com.polo42.project_flow.repository.ElementRepository;
import com.polo42.project_flow.repository.ProjectTaskCountProjection;
import com.polo42.project_flow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DiagramNodeRepository diagramNodeRepository;

    @Autowired
    private DiagramConnectionRepository diagramConnectionRepository;

    @Autowired
    private DiagramViewStateRepository diagramViewStateRepository;

    @Autowired
    private ElementRepository elementRepository;

    // 🔹 Buscar projeto por ID
    @Transactional(readOnly = true)
    public ProjectDTO findById(UUID id, UUID currentUserId) {
        Project project = findOwned(id, currentUserId);
        int totalTarefas = Math.toIntExact(diagramNodeRepository.countByProjetoId(project.getId()));
        return convertToDTO(project, totalTarefas);
    }

    // 🔹 Listar projetos de um usuário
    @Transactional(readOnly = true)
    public List<ProjectDTO> findByUsuarioId(UUID usuarioId, UUID currentUserId) {
        if (!usuarioId.equals(currentUserId) || !userRepository.existsById(usuarioId)) {
            throw new ResourceNotFoundException("Usuário", "id", usuarioId.toString());
        }

        List<Project> projects = projectRepository.findByUsuarioId(usuarioId);
        Map<UUID, Integer> totalsByProject = loadTaskCounts(projects);

        return projects.stream()
            .map(project -> convertToDTO(project, totalsByProject.getOrDefault(project.getId(), 0)))
            .collect(Collectors.toList());
    }

    // 🔹 Criar novo projeto
    @Transactional
    public ProjectDTO create(UUID usuarioId, UUID currentUserId, ProjectDTO projectDTO) {
        if (!usuarioId.equals(currentUserId)) {
            throw new ResourceNotFoundException("Usuário", "id", usuarioId.toString());
        }
        User usuario = userRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "id", usuarioId.toString()));

        Project project = new Project();
        project.setNome(projectDTO.getNome());
        project.setDescricao(projectDTO.getDescricao());
        project.setUsuario(usuario);
        project.setDataCriado(LocalDateTime.now());
        project.setUltimaAlteracao(LocalDateTime.now());
        project.setUltimoAcesso(LocalDateTime.now());

        Project savedProject = projectRepository.save(project);
        return convertToDTO(savedProject, 0);
    }

    // 🔹 Atualizar projeto
    @Transactional
    public ProjectDTO update(UUID id, UUID currentUserId, ProjectDTO projectDTO) {
        Project project = findOwned(id, currentUserId);

        project.setNome(projectDTO.getNome());
        project.setDescricao(projectDTO.getDescricao());
        project.setUltimaAlteracao(LocalDateTime.now());

        Project updatedProject = projectRepository.save(project);
        int totalTarefas = Math.toIntExact(diagramNodeRepository.countByProjetoId(updatedProject.getId()));
        return convertToDTO(updatedProject, totalTarefas);
    }

    // 🔹 Deletar projeto
    @Transactional
    public void delete(UUID id, UUID currentUserId) {
        findOwned(id, currentUserId);
        diagramConnectionRepository.deleteByProjetoId(id);
        diagramNodeRepository.deleteByProjetoId(id);
        diagramViewStateRepository.deleteByProjetoId(id);
        elementRepository.deleteByProjetoId(id);
        projectRepository.deleteById(id);
    }

    // Projeto de outro usuário é tratado como inexistente (404)
    private Project findOwned(UUID id, UUID currentUserId) {
        return projectRepository.findByIdAndUsuarioId(id, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", "id", id.toString()));
    }

    // 🔹 Converter Project para ProjectDTO
    private Map<UUID, Integer> loadTaskCounts(List<Project> projects) {
        if (projects == null || projects.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UUID> projectIds = projects.stream()
                .map(Project::getId)
                .collect(Collectors.toList());

        List<ProjectTaskCountProjection> aggregates = diagramNodeRepository.countTasksByProjetoIds(projectIds);
        if (aggregates == null || aggregates.isEmpty()) {
            return Collections.emptyMap();
        }

        return aggregates.stream()
                .collect(Collectors.toMap(
                        ProjectTaskCountProjection::getProjetoId,
                projection -> Math.toIntExact(projection.getTotal()),
                (existing, replacement) -> existing));
    }

    private ProjectDTO convertToDTO(Project project, int totalTarefas) {
        return new ProjectDTO(
            project.getId(),
            project.getNome(),
            project.getDescricao(),
            project.getDataCriado(),
            project.getUltimaAlteracao(),
            project.getUltimoAcesso(),
            project.getUsuario().getId(),
            project.getUsuario().getNome(),
            totalTarefas
        );
    }

}