package com.polo42.project_flow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

public class ProjectDTO {
    private UUID id;
    @NotBlank(message = "Nome do projeto é obrigatório")
    @Size(max = 100, message = "Nome do projeto não pode ultrapassar 100 caracteres")
    private String nome;
    @Size(max = 10000, message = "Descrição não pode ultrapassar 10.000 caracteres")
    private String descricao;
    private LocalDateTime dataCriado;
    private LocalDateTime ultimaAlteracao;
    private LocalDateTime ultimoAcesso;
    private UUID usuarioId;
    private String usuarioNome;
    private int totalTarefas;

    // Construtores
    public ProjectDTO() {}
    
    public ProjectDTO(UUID id, String nome, String descricao, LocalDateTime dataCriado,
                     LocalDateTime ultimaAlteracao, LocalDateTime ultimoAcesso,
                     UUID usuarioId, String usuarioNome, int totalTarefas) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.dataCriado = dataCriado;
        this.ultimaAlteracao = ultimaAlteracao;
        this.ultimoAcesso = ultimoAcesso;
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
    public LocalDateTime getUltimoAcesso() { return ultimoAcesso; }
    public void setUltimoAcesso(LocalDateTime ultimoAcesso) { this.ultimoAcesso = ultimoAcesso; }
    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }
    public String getUsuarioNome() { return usuarioNome; }
    public void setUsuarioNome(String usuarioNome) { this.usuarioNome = usuarioNome; }
    public int getTotalTarefas() { return totalTarefas; }
    public void setTotalTarefas(int totalTarefas) { this.totalTarefas = totalTarefas; }
}