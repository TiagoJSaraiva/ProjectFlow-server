package com.polo42.project_flow.repository;

import com.polo42.project_flow.model.DiagramViewState;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DiagramViewStateRepository extends JpaRepository<DiagramViewState, UUID> {

    Optional<DiagramViewState> findByProjetoId(UUID projetoId);

    @Modifying(clearAutomatically = true)
    @Query("delete from DiagramViewState v where v.projeto.id = :projetoId")
    void deleteByProjetoId(@Param("projetoId") UUID projetoId);
}
