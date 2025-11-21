package com.polo42.project_flow.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "projetos")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(name = "data_criado", nullable = false)
    private LocalDateTime dataCriado = LocalDateTime.now();

    @Column(name = "ultima_alteracao")
    private LocalDateTime ultimaAlteracao = LocalDateTime.now();

    // Muitos projetos pertencem a um usuário
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private User usuario;

    // Um projeto pode ter vários elementos
    @OneToMany(mappedBy = "projeto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Element> elementos = new ArrayList<>();

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

    public User getUsuario() { return usuario; }
    public void setUsuario(User usuario) { this.usuario = usuario; }

    public List<Element> getElementos() { return elementos; }
    public void setElementos(List<Element> elementos) { this.elementos = elementos; }
}
