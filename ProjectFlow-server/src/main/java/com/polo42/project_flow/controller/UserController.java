package com.polo42.project_flow.controller;

import com.polo42.project_flow.dto.UserDTO;
import com.polo42.project_flow.security.CustomUserDetails;
import com.polo42.project_flow.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    // 🔹 GET - Listar todos os usuários
    @GetMapping
    public ResponseEntity<List<UserDTO>> listarTodos(@AuthenticationPrincipal CustomUserDetails principal) {
        List<UserDTO> usuarios = userService.findAll(principal.getId());
        return ResponseEntity.ok(usuarios);
    }

    // 🔹 GET - Buscar usuário por ID
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> buscarPorId(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        return userService.findById(id, principal.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 🔹 PUT - Atualizar usuário existente
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> atualizar(@PathVariable UUID id,
                                         @AuthenticationPrincipal CustomUserDetails principal,
                                         @RequestBody UserDTO userDTO) {
        UserDTO usuarioAtualizado = userService.update(id, principal.getId(), userDTO);
        return ResponseEntity.ok(usuarioAtualizado);
    }

    // 🔹 PUT - Atualizar senha do usuário
    @PutMapping("/{id}/password")
    public ResponseEntity<Void> atualizarSenha(@PathVariable UUID id,
                                              @AuthenticationPrincipal CustomUserDetails principal,
                                              @RequestBody String novaSenha) {
        userService.updatePassword(id, principal.getId(), novaSenha);
        return ResponseEntity.noContent().build();
    }

    // 🔹 DELETE - Excluir usuário
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        userService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}