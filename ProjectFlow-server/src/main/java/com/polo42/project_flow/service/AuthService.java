package com.polo42.project_flow.service;

import com.polo42.project_flow.dto.AuthRequestDTO;
import com.polo42.project_flow.dto.AuthResponseDTO;
import com.polo42.project_flow.dto.CreateUserDTO;
import com.polo42.project_flow.dto.UserDTO;
import com.polo42.project_flow.exception.BusinessException;
import com.polo42.project_flow.exception.ResourceNotFoundException;
import com.polo42.project_flow.model.User;
import com.polo42.project_flow.repository.UserRepository;
import com.polo42.project_flow.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private EmailVerificationService emailVerificationService;

    // 🔹 Registrar novo usuário
    public UserDTO register(CreateUserDTO createUserDTO) {
        // Verificar se email já existe
        if (userRepository.existsByEmail(createUserDTO.getEmail())) {
            throw new BusinessException("Email já está em uso");
        }

        // Criar novo usuário
        User user = new User();
        user.setNome(createUserDTO.getNome());
        user.setEmail(createUserDTO.getEmail());
        user.setSenha(passwordEncoder.encode(createUserDTO.getSenha()));
        user.setDataCriado(LocalDateTime.now());
        user.setEmailVerificado(false);
        user.setDataVerificado(null);

        User savedUser = userRepository.save(user);
        try {
            emailVerificationService.sendVerificationEmail(savedUser);
        } catch (RuntimeException ex) {
            userRepository.deleteById(savedUser.getId());
            throw ex;
        }

        return convertToDTO(savedUser);
    }

    // 🔹 Login de usuário
    public AuthResponseDTO login(AuthRequestDTO authRequestDTO) {
        try {
            // Autenticar usuário
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    authRequestDTO.getEmail(),
                    authRequestDTO.getSenha()
                )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Gerar token JWT
            String jwt = tokenProvider.generateToken(authentication);

            // Buscar dados do usuário
            User user = userRepository.findByEmail(authRequestDTO.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário", "email", authRequestDTO.getEmail()));

            if (!user.isEmailVerificado()) {
                throw new BusinessException("Confirme seu email antes de acessar.");
            }

            // Retornar resposta com token
            return new AuthResponseDTO(
                jwt,
                user.getId(),
                user.getNome(),
                user.getEmail()
            );
        } catch (BadCredentialsException ex) {
            throw new BusinessException("Email ou senha inválidos");
        }
    }

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