package com.polo42.project_flow.repository;

import com.polo42.project_flow.model.Project;
import com.polo42.project_flow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    // Lista todos os projetos de um usuário específico
    List<Project> findByUsuarioId(UUID userId);
    
    // Busca um projeto apenas se pertencer ao usuario informado
    Optional<Project> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    // Lista projetos associados a uma instância de User
    List<Project> findByUsuario(User usuario);
}