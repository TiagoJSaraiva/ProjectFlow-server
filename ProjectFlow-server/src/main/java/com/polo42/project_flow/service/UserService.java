package com.polo42.project_flow.service;

import com.polo42.project_flow.dto.UserDTO;
import com.polo42.project_flow.exception.ResourceNotFoundException;
import com.polo42.project_flow.exception.BusinessException;
import com.polo42.project_flow.model.User;
import com.polo42.project_flow.repository.DiagramConnectionRepository;
import com.polo42.project_flow.repository.DiagramNodeRepository;
import com.polo42.project_flow.repository.DiagramViewStateRepository;
import com.polo42.project_flow.repository.ElementRepository;
import com.polo42.project_flow.repository.EmailVerificationTokenRepository;
import com.polo42.project_flow.repository.PasswordResetTokenRepository;
import com.polo42.project_flow.repository.ProjectRepository;
import com.polo42.project_flow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DiagramConnectionRepository connectionRepository;

    @Autowired
    private DiagramNodeRepository nodeRepository;

    @Autowired
    private DiagramViewStateRepository viewStateRepository;

    @Autowired
    private ElementRepository elementRepository;

    @Autowired
    private EmailVerificationTokenRepository emailTokenRepository;

    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;

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
    @Transactional
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
    @Transactional
    public void updatePassword(UUID id, UUID currentUserId, String newPassword) {
        User user = findSelf(id, currentUserId);

        user.setSenha(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // 🔹 Deletar usuário
    // Remove, em uma unica transacao, a conta e todos os dados do usuario (as FKs nao tem cascade no banco).
    @Transactional
    public void delete(UUID id, UUID currentUserId) {
        findSelf(id, currentUserId);

        projectRepository.findByUsuarioId(id).forEach(project -> {
            UUID projectId = project.getId();
            connectionRepository.deleteByProjetoId(projectId);
            nodeRepository.deleteByProjetoId(projectId);
            viewStateRepository.deleteByProjetoId(projectId);
            elementRepository.deleteByProjetoId(projectId);
        });
        projectRepository.deleteByUsuarioId(id);
        emailTokenRepository.deleteByUserId(id);
        resetTokenRepository.deleteByUserId(id);
        userRepository.deleteUserById(id);
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