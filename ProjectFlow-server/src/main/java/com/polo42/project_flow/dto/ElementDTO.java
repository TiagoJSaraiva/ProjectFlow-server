package com.polo42.project_flow.dto;

import com.polo42.project_flow.model.ElementType;
import java.time.LocalDateTime;
import java.util.UUID;

public class ElementDTO {
    private UUID id;
    private String nome;
    private String descricao;
    private String conteudo;
    private ElementType tipo;
    private String coordenadas;
    private String relacoes;
    private LocalDateTime dataCriado;
    private LocalDateTime ultimaAlteracao;
    private UUID projetoId;

    // Construtores
    public ElementDTO() {}
    
    public ElementDTO(UUID id, String nome, String descricao, String conteudo, ElementType tipo, 
                     String coordenadas, String relacoes, LocalDateTime dataCriado, 
                     LocalDateTime ultimaAlteracao, UUID projetoId) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.conteudo = conteudo;
        this.tipo = tipo;
        this.coordenadas = coordenadas;
        this.relacoes = relacoes;
        this.dataCriado = dataCriado;
        this.ultimaAlteracao = ultimaAlteracao;
        this.projetoId = projetoId;
    }

    // Getters e Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getConteudo() { return conteudo; }
    public void setConteudo(String conteudo) { this.conteudo = conteudo; }
    public ElementType getTipo() { return tipo; }
    public void setTipo(ElementType tipo) { this.tipo = tipo; }
    public String getCoordenadas() { return coordenadas; }
    public void setCoordenadas(String coordenadas) { this.coordenadas = coordenadas; }
    public String getRelacoes() { return relacoes; }
    public void setRelacoes(String relacoes) { this.relacoes = relacoes; }
    public LocalDateTime getDataCriado() { return dataCriado; }
    public void setDataCriado(LocalDateTime dataCriado) { this.dataCriado = dataCriado; }
    public LocalDateTime getUltimaAlteracao() { return ultimaAlteracao; }
    public void setUltimaAlteracao(LocalDateTime ultimaAlteracao) { this.ultimaAlteracao = ultimaAlteracao; }
    public UUID getProjetoId() { return projetoId; }
    public void setProjetoId(UUID projetoId) { this.projetoId = projetoId; }
}