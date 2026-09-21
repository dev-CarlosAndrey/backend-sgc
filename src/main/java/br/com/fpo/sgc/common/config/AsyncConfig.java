package br.com.fpo.sgc.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// Habilita @Async (usado no disparo de notificações, para não atrasar a resposta da API).
@Configuration
@EnableAsync
public class AsyncConfig {
}
