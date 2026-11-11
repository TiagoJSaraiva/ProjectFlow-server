package com.polo42.project_flow.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A validacao de inicializacao recusa segredo ausente, curto ou de exemplo.
 */
class JwtSecretValidationTest {

    private JwtTokenProvider providerWith(String secret) {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", secret);
        ReflectionTestUtils.setField(provider, "jwtExpiration", 1000);
        return provider;
    }

    @Test
    void recusaSegredoVazioOuAusente() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> providerWith("").validateSecret());
        assertTrue(ex.getMessage().contains("JWT_SECRET"));
        assertThrows(IllegalStateException.class, () -> providerWith(null).validateSecret());
        assertThrows(IllegalStateException.class, () -> providerWith("   ").validateSecret());
    }

    @Test
    void recusaSegredoCurto() {
        assertThrows(IllegalStateException.class, () -> providerWith("curto-demais").validateSecret());
    }

    @Test
    void recusaValoresDeExemplo() {
        String exemplo = "troque-este-segredo-ficticio-por-um-valor-aleatorio-com-pelo-menos-64-caracteres";
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> providerWith(exemplo).validateSecret());
        assertTrue(ex.getMessage().contains("exemplo"));
        assertThrows(IllegalStateException.class,
                () -> providerWith("changeme".repeat(10)).validateSecret());
    }

    @Test
    void aceitaSegredoAleatorioComPeloMenos64Caracteres() {
        String secret = "k3Jf9sLq0ZxV7mPb2RtYw8NdHc5UaEg1OiXyQ4TzBvM6nWjFhD+KlSpCrAe/GuIo9sLq0ZxV7mPb2";
        assertDoesNotThrow(() -> providerWith(secret).validateSecret());
    }
}
