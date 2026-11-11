package com.polo42.project_flow.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "usuarios")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Column(columnDefinition = "json")
    private String metadados;

    @Column(name = "data_criado", nullable = false)
    private LocalDateTime dataCriado = LocalDateTime.now();

    @Column(name = "email_verificado", nullable = false)
    private boolean emailVerificado = false;

    @Column(name = "data_verificado")
    private LocalDateTime dataVerificado;

    // Carimbo da ultima troca/redefinicao de senha; tokens JWT emitidos antes dele deixam de valer
    @Column(name = "senha_alterada_em")
    private Instant senhaAlteradaEm;

    // Um usuário pode ter vários projetos
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Project> projetos = new ArrayList<>();

    // Getters e Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getMetadados() { return metadados; }
    public void setMetadados(String metadados) { this.metadados = metadados; }

    public LocalDateTime getDataCriado() { return dataCriado; }
    public void setDataCriado(LocalDateTime dataCriado) { this.dataCriado = dataCriado; }

    public Instant getSenhaAlteradaEm() { return senhaAlteradaEm; }
    public void setSenhaAlteradaEm(Instant senhaAlteradaEm) { this.senhaAlteradaEm = senhaAlteradaEm; }

    public List<Project> getProjetos() { return projetos; }
    public void setProjetos(List<Project> projetos) { this.projetos = projetos; }

    public boolean isEmailVerificado() { return emailVerificado; }
    public void setEmailVerificado(boolean emailVerificado) { this.emailVerificado = emailVerificado; }

    public LocalDateTime getDataVerificado() { return dataVerificado; }
    public void setDataVerificado(LocalDateTime dataVerificado) { this.dataVerificado = dataVerificado; }
}
