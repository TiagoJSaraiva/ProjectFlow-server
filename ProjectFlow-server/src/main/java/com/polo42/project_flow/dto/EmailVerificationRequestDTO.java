package com.polo42.project_flow.dto;

import jakarta.validation.constraints.NotBlank;

public class EmailVerificationRequestDTO {

    @NotBlank
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
