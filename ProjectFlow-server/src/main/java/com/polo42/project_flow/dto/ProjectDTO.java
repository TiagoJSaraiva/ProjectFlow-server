package com.polo42.project_flow.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class ProjectDTO {
    private UUID id;
    private String nome;
    private String descricao;
    private LocalDateTime dataCriado;
    private LocalDateTime ultimaAlteracao;
    private UUID usuarioId;
    private String usuarioNome;
    private int totalTarefas;

    // Construtores
    public ProjectDTO() {}
    
    public ProjectDTO(UUID id, String nome, String descricao, LocalDateTime dataCriado, 
                     LocalDateTime ultimaAlteracao, UUID usuarioId, String usuarioNome, int totalTarefas) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.dataCriado = dataCriado;
        this.ultimaAlteracao = ultimaAlteracao;
        this.usuarioId = usuarioId;
        this.usuarioNome = usuarioNome;
        this.totalTarefas = totalTarefas;
    }

    // Getters e Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public LocalDateTime getDataCriado() { return dataCriado; }
    public void setDataCriado(LocalDateTime dataCriado) { this.dataCriado = dataCriado; }
    public LocalDateTime getUltimaAlteracao() { return ultimaAlteracao; }
    public void setUltimaAlteracao(LocalDateTime ultimaAlteracao) { this.ultimaAlteracao = ultimaAlteracao; }
    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }
    public String getUsuarioNome() { return usuarioNome; }
    public void setUsuarioNome(String usuarioNome) { this.usuarioNome = usuarioNome; }
    public int getTotalTarefas() { return totalTarefas; }
    public void setTotalTarefas(int totalTarefas) { this.totalTarefas = totalTarefas; }
}