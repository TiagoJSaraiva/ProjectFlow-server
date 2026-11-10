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
    public Optional<UserDTO> findById(UUID id, UUID currentUserId) {
        if (!id.equals(currentUserId)) {
            return Optional.empty();
        }
        return userRepository.findById(id)
                .map(this::convertToDTO);
    }

    // 🔹 Buscar usuário por email
    public Optional<UserDTO> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::convertToDTO);
    }

    // 🔹 Listar usuários: um usuário só enxerga a própria conta
    public List<UserDTO> findAll(UUID currentUserId) {
        return userRepository.findById(currentUserId)
                .map(this::convertToDTO)
                .stream()
                .collect(Collectors.toList());
    }

    // 🔹 Atualizar usuário
    public UserDTO update(UUID id, UUID currentUserId, UserDTO userDTO) {
        User user = findSelf(id, currentUserId);

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
    public void updatePassword(UUID id, UUID currentUserId, String newPassword) {
        User user = findSelf(id, currentUserId);

        user.setSenha(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // 🔹 Deletar usuário
    public void delete(UUID id, UUID currentUserId) {
        findSelf(id, currentUserId);
        userRepository.deleteById(id);
    }

    // Conta de outro usuário é tratada como inexistente (404)
    private User findSelf(UUID id, UUID currentUserId) {
        if (!id.equals(currentUserId)) {
            throw new ResourceNotFoundException("Usuário", "id", id.toString());
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "id", id.toString()));
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