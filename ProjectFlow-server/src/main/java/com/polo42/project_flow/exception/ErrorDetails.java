package com.polo42.project_flow.exception;

import java.time.LocalDateTime;

public class ErrorDetails {
    private LocalDateTime timestamp;
    private String message;
    private String details;
    private int status;
    private String error;

    public ErrorDetails(LocalDateTime timestamp, String message, String details, int status, String error) {
        this.timestamp = timestamp;
        this.message = message;
        this.details = details;
        this.status = status;
        this.error = error;
    }

    // Getters
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getMessage() { return message; }
    public String getDetails() { return details; }
    public int getStatus() { return status; }
    public String getError() { return error; }
}