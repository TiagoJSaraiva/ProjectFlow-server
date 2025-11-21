package com.polo42.project_flow.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "elementos")
public class Element {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(columnDefinition = "text")
    private String conteudo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ElementType tipo;

    @Column(columnDefinition = "json")
    private String coordenadas;

    @Column(columnDefinition = "json")
    private String relacoes;

    @Column(name = "data_criado", nullable = false)
    private LocalDateTime dataCriado = LocalDateTime.now();

    @Column(name = "ultima_alteracao")
    private LocalDateTime ultimaAlteracao = LocalDateTime.now();

    // Muitos elementos pertencem a um projeto
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_projeto", nullable = false)
    private Project projeto;

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

    public Project getProjeto() { return projeto; }
    public void setProjeto(Project projeto) { this.projeto = projeto; }
}
