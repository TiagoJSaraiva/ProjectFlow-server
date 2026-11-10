package com.polo42.project_flow.repository;

import com.polo42.project_flow.model.Element;
import com.polo42.project_flow.model.Project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ElementRepository extends JpaRepository<Element, UUID> {

    // Lista todos os elementos de um projeto
    List<Element> findByProjeto(Project projeto);
    
    // Lista elementos por ID do projeto (usando UUID)
    List<Element> findByProjetoId(UUID projectId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Element e where e.projeto.id = :projetoId")
    void deleteByProjetoId(@Param("projetoId") UUID projetoId);
}