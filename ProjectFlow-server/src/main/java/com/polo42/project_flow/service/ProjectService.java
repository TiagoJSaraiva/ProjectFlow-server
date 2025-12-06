package com.polo42.project_flow.service;

import com.polo42.project_flow.dto.ProjectDTO;
import com.polo42.project_flow.exception.ResourceNotFoundException;
import com.polo42.project_flow.model.Project;
import com.polo42.project_flow.model.User;
import com.polo42.project_flow.repository.ProjectRepository;
import com.polo42.project_flow.repository.DiagramNodeRepository;
import com.polo42.project_flow.repository.DiagramConnectionRepository;
import com.polo42.project_flow.repository.DiagramViewStateRepository;
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

    // 🔹 Buscar projeto por ID
    @Transactional(readOnly = true)
    public ProjectDTO findById(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", "id", id.toString()));
        int totalTarefas = Math.toIntExact(diagramNodeRepository.countByProjetoId(project.getId()));
        return convertToDTO(project, totalTarefas);
    }

    // 🔹 Listar projetos de um usuário
    @Transactional(readOnly = true)
    public List<ProjectDTO> findByUsuarioId(UUID usuarioId) {
        if (!userRepository.existsById(usuarioId)) {
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
    public ProjectDTO create(UUID usuarioId, ProjectDTO projectDTO) {
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
    public ProjectDTO update(UUID id, ProjectDTO projectDTO) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", "id", id.toString()));

        project.setNome(projectDTO.getNome());
        project.setDescricao(projectDTO.getDescricao());
        project.setUltimaAlteracao(LocalDateTime.now());

        Project updatedProject = projectRepository.save(project);
        int totalTarefas = Math.toIntExact(diagramNodeRepository.countByProjetoId(updatedProject.getId()));
        return convertToDTO(updatedProject, totalTarefas);
    }

    // 🔹 Deletar projeto
    @Transactional
    public void delete(UUID id) {
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Projeto", "id", id.toString());
        }
        diagramConnectionRepository.deleteByProjetoId(id);
        diagramNodeRepository.deleteByProjetoId(id);
        diagramViewStateRepository.deleteByProjetoId(id);
        projectRepository.deleteById(id);
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