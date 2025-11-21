package com.polo42.project_flow.service;

import com.polo42.project_flow.dto.UserDTO;
import com.polo42.project_flow.exception.ResourceNotFoundException;
import com.polo42.project_flow.exception.BusinessException;
import com.polo42.project_flow.model.User;
import com.polo42.project_flow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 🔹 Buscar usuário por ID
    public Optional<UserDTO> findById(UUID id) {
        return userRepository.findById(id)
                .map(this::convertToDTO);
    }

    // 🔹 Buscar usuário por email
    public Optional<UserDTO> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::convertToDTO);
    }

    // 🔹 Listar todos os usuários
    public List<UserDTO> findAll() {
        return userRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 🔹 Atualizar usuário
    public UserDTO update(UUID id, UserDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "id", id.toString()));

        // Verificar se o email já está em uso por outro usuário
        if (userRepository.existsByEmailAndIdNot(userDTO.getEmail(), id)) {
            throw new BusinessException("Email já está em uso por outro usuário");
        }

        user.setNome(userDTO.getNome());
        user.setEmail(userDTO.getEmail());
        
        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    // 🔹 Atualizar senha
    public void updatePassword(UUID id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "id", id.toString()));

        user.setSenha(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // 🔹 Deletar usuário
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuário", "id", id.toString());
        }
        userRepository.deleteById(id);
    }

    // 🔹 Converter User para UserDTO
    private UserDTO convertToDTO(User user) {
        return new UserDTO(
            user.getId(),
            user.getNome(),
            user.getEmail(),
            user.getDataCriado(),
            user.isEmailVerificado(),
            user.getDataVerificado()
        );
    }
}