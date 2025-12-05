package com.polo42.project_flow.repository;

import com.polo42.project_flow.model.DiagramViewState;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiagramViewStateRepository extends JpaRepository<DiagramViewState, UUID> {

    Optional<DiagramViewState> findByProjetoId(UUID projetoId);
}
