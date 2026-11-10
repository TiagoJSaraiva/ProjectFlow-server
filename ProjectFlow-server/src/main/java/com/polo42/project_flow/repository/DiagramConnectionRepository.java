package com.polo42.project_flow.repository;

import com.polo42.project_flow.model.DiagramConnection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DiagramConnectionRepository extends JpaRepository<DiagramConnection, UUID> {

    List<DiagramConnection> findByProjetoId(UUID projetoId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from DiagramConnection c where c.projeto.id = :projetoId")
    void deleteByProjetoId(@Param("projetoId") UUID projetoId);
}
