package com.polo42.project_flow.service;

import com.polo42.project_flow.dto.ProjectDTO;
import com.polo42.project_flow.exception.ResourceNotFoundException;
import com.polo42.project_flow.model.Project;
import com.polo42.project_flow.model.User;
import com.polo42.project_flow.repository.ProjectRepository;
import com.polo42.project_flow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    // 🔹 Buscar projeto por ID
    @Transactional(readOnly = true)
    public ProjectDTO findById(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", "id", id.toString()));
        return convertToDTO(project);
    }

    // 🔹 Listar projetos de um usuário
    @Transactional(readOnly = true)
    public List<ProjectDTO> findByUsuarioId(UUID usuarioId) {
        if (!userRepository.existsById(usuarioId)) {
            throw new ResourceNotFoundException("Usuário", "id", usuarioId.toString());
        }

        return projectRepository.findByUsuarioId(usuarioId)
                .stream()
                .map(this::convertToDTO)
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

        Project savedProject = projectRepository.save(project);
        return convertToDTO(savedProject);
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
        return convertToDTO(updatedProject);
    }

    // 🔹 Deletar projeto
    @Transactional
    public void delete(UUID id) {
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Projeto", "id", id.toString());
        }
        projectRepository.deleteById(id);
    }

    // 🔹 Converter Project para ProjectDTO
    private ProjectDTO convertToDTO(Project project) {
        return new ProjectDTO(
            project.getId(),
            project.getNome(),
            project.getDescricao(),
            project.getDataCriado(),
            project.getUltimaAlteracao(),
            project.getUsuario().getId(),
            project.getUsuario().getNome(),
            project.getElementos() != null ? project.getElementos().size() : 0
        );
    }
}