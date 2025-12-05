package com.polo42.project_flow.repository;

import com.polo42.project_flow.model.DiagramNode;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DiagramNodeRepository extends JpaRepository<DiagramNode, UUID> {

    List<DiagramNode> findByProjetoIdOrderByCreatedAtAsc(UUID projetoId);

    @Modifying
    @Query("delete from DiagramNode n where n.projeto.id = :projetoId")
    void deleteByProjetoId(UUID projetoId);
}
