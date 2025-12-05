package com.polo42.project_flow.dto;

import java.util.Map;
import java.util.UUID;

public class DiagramConnectionDTO {
    private UUID id;
    private UUID fromNodeId;
    private UUID toNodeId;
    private DiagramPointDTO bendPoint;
    private Map<String, Object> style;
    private Map<String, Object> metadata;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getFromNodeId() {
        return fromNodeId;
    }

    public void setFromNodeId(UUID fromNodeId) {
        this.fromNodeId = fromNodeId;
    }

    public UUID getToNodeId() {
        return toNodeId;
    }

    public void setToNodeId(UUID toNodeId) {
        this.toNodeId = toNodeId;
    }

    public DiagramPointDTO getBendPoint() {
        return bendPoint;
    }

    public void setBendPoint(DiagramPointDTO bendPoint) {
        this.bendPoint = bendPoint;
    }

    public Map<String, Object> getStyle() {
        return style;
    }

    public void setStyle(Map<String, Object> style) {
        this.style = style;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
