package com.polo42.project_flow.repository;

import java.util.UUID;

public interface ProjectTaskCountProjection {
    UUID getProjetoId();
    long getTotal();
}
