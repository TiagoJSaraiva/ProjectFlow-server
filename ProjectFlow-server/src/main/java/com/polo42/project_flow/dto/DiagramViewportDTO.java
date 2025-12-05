package com.polo42.project_flow.dto;

public class DiagramViewportDTO {
    private double zoom;
    private double offsetX;
    private double offsetY;
    private double gridSize;

    public DiagramViewportDTO() {
    }

    public DiagramViewportDTO(double zoom, double offsetX, double offsetY, double gridSize) {
        this.zoom = zoom;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.gridSize = gridSize;
    }

    public double getZoom() {
        return zoom;
    }

    public void setZoom(double zoom) {
        this.zoom = zoom;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(double offsetX) {
        this.offsetX = offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(double offsetY) {
        this.offsetY = offsetY;
    }

    public double getGridSize() {
        return gridSize;
    }

    public void setGridSize(double gridSize) {
        this.gridSize = gridSize;
    }
}
