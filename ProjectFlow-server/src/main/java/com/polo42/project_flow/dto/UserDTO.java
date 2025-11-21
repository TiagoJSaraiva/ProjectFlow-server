package com.polo42.project_flow.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserDTO {
    private UUID id;
    private String nome;
    private String email;
    private LocalDateTime dataCriado;
    private boolean emailVerificado;
    private LocalDateTime dataVerificado;

    // Construtores
    public UserDTO() {}
    
    public UserDTO(UUID id, String nome, String email, LocalDateTime dataCriado, boolean emailVerificado, LocalDateTime dataVerificado) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.dataCriado = dataCriado;
        this.emailVerificado = emailVerificado;
        this.dataVerificado = dataVerificado;
    }

    // Getters e Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDateTime getDataCriado() { return dataCriado; }
    public void setDataCriado(LocalDateTime dataCriado) { this.dataCriado = dataCriado; }
    public boolean isEmailVerificado() { return emailVerificado; }
    public void setEmailVerificado(boolean emailVerificado) { this.emailVerificado = emailVerificado; }
    public LocalDateTime getDataVerificado() { return dataVerificado; }
    public void setDataVerificado(LocalDateTime dataVerificado) { this.dataVerificado = dataVerificado; }
}