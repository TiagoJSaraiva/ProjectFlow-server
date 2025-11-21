package com.polo42.project_flow.dto;

import java.util.UUID;

public class AuthResponseDTO {
    private String token;
    private String tipo = "Bearer";
    private UUID userId;
    private String nome;
    private String email;

    // Construtores
    public AuthResponseDTO() {}
    
    public AuthResponseDTO(String token, UUID userId, String nome, String email) {
        this.token = token;
        this.userId = userId;
        this.nome = nome;
        this.email = email;
    }

    // Getters e Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}