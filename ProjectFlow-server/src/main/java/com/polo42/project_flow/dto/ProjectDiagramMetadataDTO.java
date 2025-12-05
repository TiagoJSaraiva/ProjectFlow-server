package com.polo42.project_flow.dto;

public class ProjectDiagramMetadataDTO {
    private String name;
    private String description;

    public ProjectDiagramMetadataDTO() {
    }

    public ProjectDiagramMetadataDTO(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
