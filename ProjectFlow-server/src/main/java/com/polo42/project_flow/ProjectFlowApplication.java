package com.polo42.project_flow;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class ProjectFlowApplication {

    /**
     * Todas as datas (LocalDateTime) sao geradas e gravadas em UTC: o frontend interpreta as strings
     * ISO-8601 sem fuso como UTC. Fixado no PostConstruct (vale tambem nos testes) e no main.
     */
    @PostConstruct
    void useUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        // .env opcional no diretorio de trabalho; variaveis ja definidas no ambiente prevalecem
        EnvFileLoader.load();

        SpringApplication.run(ProjectFlowApplication.class, args);
    }
}