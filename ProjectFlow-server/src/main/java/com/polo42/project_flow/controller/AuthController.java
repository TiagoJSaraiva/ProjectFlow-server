package com.polo42.project_flow.controller;

import com.polo42.project_flow.dto.AuthRequestDTO;
import com.polo42.project_flow.dto.AuthResponseDTO;
import com.polo42.project_flow.dto.CreateUserDTO;
import com.polo42.project_flow.dto.EmailOnlyRequestDTO;
import com.polo42.project_flow.dto.EmailVerificationRequestDTO;
import com.polo42.project_flow.dto.PasswordResetConfirmDTO;
import com.polo42.project_flow.dto.PasswordResetRequestDTO;
import com.polo42.project_flow.dto.UserDTO;
import com.polo42.project_flow.service.AuthService;
import com.polo42.project_flow.service.EmailVerificationService;
import com.polo42.project_flow.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private EmailVerificationService emailVerificationService;

    // 🔹 Registrar novo usuário
    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody CreateUserDTO createUserDTO) {
        UserDTO userDTO = authService.register(createUserDTO);
        return ResponseEntity.ok(userDTO);
    }

    // 🔹 Login de usuário
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO authRequestDTO) {
        AuthResponseDTO authResponse = authService.login(authRequestDTO);
        return ResponseEntity.ok(authResponse);
    }

    // 🔹 Recuperação de senha
    @PostMapping("/recover")
    public ResponseEntity<Void> recover(@Valid @RequestBody PasswordResetRequestDTO request) {
        passwordResetService.sendRecoveryEmail(request.getEmail());
        return ResponseEntity.ok().build();
    }

    // 🔹 Confirmar redefinição de senha
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetConfirmDTO request) {
        passwordResetService.resetPassword(request.getToken(), request.getNovaSenha());
        return ResponseEntity.ok().build();
    }

    // 🔹 Confirmar email via token
    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody EmailVerificationRequestDTO request) {
        emailVerificationService.verifyToken(request.getToken());
        return ResponseEntity.ok().build();
    }

    // 🔹 Reenviar email de verificação
    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody EmailOnlyRequestDTO request) {
        emailVerificationService.resendVerification(request.getEmail());
        return ResponseEntity.ok().build();
    }
}