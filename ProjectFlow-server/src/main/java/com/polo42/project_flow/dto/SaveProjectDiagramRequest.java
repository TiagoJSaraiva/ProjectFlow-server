package com.polo42.project_flow.dto;

import java.util.ArrayList;
import java.util.List;

public class SaveProjectDiagramRequest {
    private ProjectDiagramMetadataDTO project;
    private DiagramViewportDTO viewport;
    private List<DiagramNodeDTO> nodes = new ArrayList<>();
    private List<DiagramConnectionDTO> connections = new ArrayList<>();

    public ProjectDiagramMetadataDTO getProject() {
        return project;
    }

    public void setProject(ProjectDiagramMetadataDTO project) {
        this.project = project;
    }

    public DiagramViewportDTO getViewport() {
        return viewport;
    }

    public void setViewport(DiagramViewportDTO viewport) {
        this.viewport = viewport;
    }

    public List<DiagramNodeDTO> getNodes() {
        return nodes;
    }

    public void setNodes(List<DiagramNodeDTO> nodes) {
        this.nodes = nodes;
    }

    public List<DiagramConnectionDTO> getConnections() {
        return connections;
    }

    public void setConnections(List<DiagramConnectionDTO> connections) {
        this.connections = connections;
    }
}
