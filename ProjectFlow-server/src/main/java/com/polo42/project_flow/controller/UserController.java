package com.polo42.project_flow.controller;

import com.polo42.project_flow.dto.UserDTO;
import com.polo42.project_flow.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<UserDTO>> listarTodos() {
        List<UserDTO> usuarios = userService.findAll();
        return ResponseEntity.ok(usuarios);
    }

    // 🔹 GET - Buscar usuário por ID
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> buscarPorId(@PathVariable UUID id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 🔹 PUT - Atualizar usuário existente
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> atualizar(@PathVariable UUID id, @RequestBody UserDTO userDTO) {
        UserDTO usuarioAtualizado = userService.update(id, userDTO);
        return ResponseEntity.ok(usuarioAtualizado);
    }

    // 🔹 PUT - Atualizar senha do usuário
    @PutMapping("/{id}/password")
    public ResponseEntity<Void> atualizarSenha(@PathVariable UUID id, @RequestBody String novaSenha) {
        userService.updatePassword(id, novaSenha);
        return ResponseEntity.noContent().build();
    }

    // 🔹 DELETE - Excluir usuário
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}