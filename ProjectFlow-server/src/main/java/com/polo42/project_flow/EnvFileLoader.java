package com.polo42.project_flow;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.Map;

/**
 * Carrega o arquivo .env do diretorio de trabalho (se existir) como propriedades de sistema.
 * Valores ja definidos no ambiente (variaveis de ambiente do SO ou -D) tem prioridade sobre o .env.
 * O arquivo so e lido quando a aplicacao inicia pelo main (spring-boot:run, java -jar); os testes nao o leem.
 */
final class EnvFileLoader {

    private EnvFileLoader() {
    }

    static void load() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        Map<String, String> entries = new java.util.HashMap<>();
        dotenv.entries().forEach(entry -> entries.put(entry.getKey(), entry.getValue()));
        apply(entries);
    }

    static void apply(Map<String, String> entries) {
        entries.forEach((key, value) -> {
            if (System.getenv(key) == null && System.getProperty(key) == null) {
                System.setProperty(key, value);
            }
        });
    }
}
