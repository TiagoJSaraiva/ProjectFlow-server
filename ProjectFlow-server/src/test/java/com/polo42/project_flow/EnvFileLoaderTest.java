package com.polo42.project_flow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Valores do .env so entram quando a variavel ainda nao esta definida no ambiente.
 */
class EnvFileLoaderTest {

    private static final String NEW_KEY = "PROJECTFLOW_TEST_NEW_KEY";
    private static final String EXISTING_KEY = "PROJECTFLOW_TEST_EXISTING_KEY";

    @AfterEach
    void cleanup() {
        System.clearProperty(NEW_KEY);
        System.clearProperty(EXISTING_KEY);
    }

    @Test
    void definePropriedadeAusenteENaoSobrescreveExistente() {
        System.setProperty(EXISTING_KEY, "valor-do-ambiente");

        EnvFileLoader.apply(Map.of(NEW_KEY, "valor-do-env", EXISTING_KEY, "valor-do-env"));

        assertEquals("valor-do-env", System.getProperty(NEW_KEY));
        assertEquals("valor-do-ambiente", System.getProperty(EXISTING_KEY));
    }
}
