package com.polo42.project_flow.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProjectDiagramDTO {
    private UUID projectId;
    private String projectName;
    private String projectDescription;
    private DiagramViewportDTO viewport;
    private List<DiagramNodeDTO> nodes = new ArrayList<>();
    private List<DiagramConnectionDTO> connections = new ArrayList<>();

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
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
