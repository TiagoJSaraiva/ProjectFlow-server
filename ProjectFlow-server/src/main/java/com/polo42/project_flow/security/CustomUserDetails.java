package com.polo42.project_flow.security;

import com.polo42.project_flow.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class CustomUserDetails implements UserDetails {
    private UUID id;
    private String email;
    private String senha;
    private long passwordVersion;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.senha = user.getSenha();
        this.passwordVersion = user.getSenhaAlteradaEm() != null ? user.getSenhaAlteradaEm().toEpochMilli() : 0L;
    }

    /** Epoch em ms da ultima troca de senha (0 se nunca trocada); gravado no token como claim "pv". */
    public long getPasswordVersion() {
        return passwordVersion;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}