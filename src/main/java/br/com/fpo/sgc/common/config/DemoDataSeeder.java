package br.com.fpo.sgc.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;

/**
 * Carrega a massa de dados de demonstração (db/demo/demo_data.sql) para a apresentação do TCC.
 *
 * Só existe com o perfil "demo" ativo (SPRING_PROFILES_ACTIVE=demo), então nunca roda em produção.
 * Fica fora do Flyway de propósito: dado de demonstração não é schema, e uma migração V20 levaria
 * esses dados para qualquer ambiente. Roda uma única vez: se a empresa de demonstração (CNPJ
 * marcador) já existe, não faz nada. O script inteiro roda numa transação só.
 */
@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    static final String MARKER_CNPJ = "11.222.333/0001-81";
    private static final String SCRIPT = "db/demo/demo_data.sql";

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_companies WHERE cnpj = ?", Integer.class, MARKER_CNPJ);
        if (existing != null && existing > 0) {
            log.info("Dados de demonstração já carregados (empresa {}); nada a fazer.", MARKER_CNPJ);
            return;
        }

        log.info("Carregando dados de demonstração de {}...", SCRIPT);
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource(SCRIPT));
        populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        populator.execute(dataSource);
        log.info("Dados de demonstração carregados. Usuários de demonstração usam a senha Demo@123.");
    }
}
