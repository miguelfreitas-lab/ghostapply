package com.ghostapply.backend.support;

import com.ghostapply.backend.model.Candidatura;
import com.ghostapply.backend.repository.CandidaturaRepository;
import com.ghostapply.backend.service.CandidaturaService;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.flyway.autoconfigure.FlywayDataSource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Contextos Spring reais, schemas independentes e somente o contêiner desta classe. */
class FlywayLifecycleIT {
    private static PostgreSQLContainer postgres;

    @BeforeAll
    static void iniciarPostgresVazio() {
        postgres = new PostgresTestConfiguration().postgres();
        postgres.start();
    }

    @AfterAll
    static void pararPostgres() {
        if (postgres != null) postgres.stop();
    }

    @Test
    void bancoVazioExecutaV1OriginalEReinicializacaoNaoReaplica() {
        String schema = novoSchema();
        iniciar(schema).run(context -> {
            assertNull(context.getStartupFailure());
            Flyway flyway = context.getBean(Flyway.class);
            assertSame(context.getBean(HikariDataSource.class), flyway.getConfiguration().getDataSource());
            assertTrue(flyway.validateWithResult().validationSuccessful);
            assertEquals(1, flyway.info().applied().length);
        });
        List<Map<String, Object>> antes = historico(schema);
        assertEquals(1, antes.size());
        assertEquals("SQL", antes.getFirst().get("type"));
        assertEquals("V1__criar_candidaturas.sql", antes.getFirst().get("script"));
        assertNotNull(antes.getFirst().get("checksum"));
        assertEquals(true, antes.getFirst().get("success"));

        iniciar(schema).run(context -> assertNull(context.getStartupFailure()));
        assertEquals(antes, historico(schema), "Nova inicialização não deve alterar o histórico da V1");
    }

    @Test
    void baselineExplicitoDoLegadoPreservaTodosOsCampos() {
        String schema = novoSchema();
        prepararLegado(schema);
        List<Map<String, Object>> antes = registros(schema);
        assertEquals(1, antes.size());
        try (var dataSource = dataSource(schema)) {
            Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                    .locations("classpath:db/migration").baselineVersion("1")
                    .baselineOnMigrate(false).validateOnMigrate(true).cleanDisabled(true)
                    .load().baseline();
        }
        assertEquals(antes, registros(schema));
        iniciar(schema).run(context -> {
            assertNull(context.getStartupFailure());
            assertTrue(context.getBean(Flyway.class).validateWithResult().validationSuccessful);
        });
        assertEquals(antes, registros(schema));
        var historico = historico(schema);
        assertEquals(1, historico.size());
        assertEquals("BASELINE", historico.getFirst().get("type"));
        assertEquals("1", historico.getFirst().get("version"));
        assertNull(historico.getFirst().get("checksum"));
    }

    @Test
    void legadoSemBaselineRecusaInicializacaoSemAlterarRegistros() {
        String schema = novoSchema();
        prepararLegado(schema);
        var antes = registros(schema);
        iniciar(schema).run(context -> {
            Throwable falha = context.getStartupFailure();
            assertFalhaFlyway(falha);
            assertTrue(mensagens(falha).contains("non-empty"));
        });
        assertEquals(antes, registros(schema));
    }

    @Test
    void checksumDivergenteRecusaInicializacaoSemRepararHistorico() {
        String schema = novoSchema();
        iniciar(schema).run(context -> assertNull(context.getStartupFailure()));
        var antes = historico(schema);
        iniciar(schema).withPropertyValues("spring.flyway.locations=classpath:flyway-fixtures/checksum")
                .run(context -> {
                    Throwable falha = context.getStartupFailure();
                    assertFalhaFlyway(falha);
                    assertTrue(mensagens(falha).toLowerCase().contains("checksum"));
                });
        assertEquals(antes, historico(schema));
        try (var dataSource = dataSource(schema)) {
            assertNull(new JdbcTemplate(dataSource).queryForObject(
                    "SELECT to_regclass(?)::text", String.class, schema + ".checksum_should_not_run"));
        }
    }

    @Test
    void migracaoIncompativelImpedeInicializacaoEReverteSuaTransacao() {
        String schema = novoSchema();
        iniciar(schema).run(context -> assertNull(context.getStartupFailure()));
        try (var dataSource = dataSource(schema)) {
            new JdbcTemplate(dataSource).update("""
                    INSERT INTO candidaturas (empresa, titulo, modalidade, status)
                    VALUES ('Preservar', 'Java', 'HIBRIDO', 'SALVA')
                    """);
        }
        var antes = registros(schema);
        var historicoAntes = historico(schema);
        iniciar(schema).withPropertyValues(
                "spring.flyway.locations=classpath:db/migration,classpath:flyway-fixtures/incompatible")
                .run(context -> {
                    Throwable falha = context.getStartupFailure();
                    assertFalhaFlyway(falha);
                    boolean colunaDuplicada = false;
                    for (Throwable causa = falha; causa != null; causa = causa.getCause()) {
                        if (causa instanceof SQLException sql && "42701".equals(sql.getSQLState())) {
                            colunaDuplicada = true;
                        }
                    }
                    assertTrue(colunaDuplicada, "Falha deve resultar da coluna duplicada na V2 de teste");
                });
        assertEquals(antes, registros(schema));
        assertEquals(historicoAntes, historico(schema));
        try (var dataSource = dataSource(schema)) {
            assertNull(new JdbcTemplate(dataSource).queryForObject(
                    "SELECT to_regclass(?)::text", String.class, schema + ".flyway_failure_probe"));
        }
    }

    private static ApplicationContextRunner iniciar(String schema) {
        return new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(ExistingContainerConfiguration.class)
                .withPropertyValues(
                        "spring.profiles.active=integration",
                        "ghostapply.test.schema=" + schema,
                        "spring.jpa.properties.hibernate.default_schema=" + schema,
                        "spring.flyway.schemas=" + schema,
                        "spring.flyway.default-schema=" + schema,
                        "spring.flyway.url=jdbc:forbidden-external-override",
                        "spring.flyway.user=forbidden", "spring.flyway.password=forbidden",
                        "spring.datasource.url=jdbc:forbidden-external-override",
                        "spring.datasource.username=forbidden", "spring.datasource.password=forbidden");
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Candidatura.class)
    @EnableJpaRepositories(basePackageClasses = CandidaturaRepository.class)
    @Import(CandidaturaService.class)
    static class ExistingContainerConfiguration {
        @Bean(destroyMethod = "close")
        @FlywayDataSource
        HikariDataSource dataSource(Environment environment) {
            return FlywayLifecycleIT.dataSource(environment.getRequiredProperty("ghostapply.test.schema"));
        }
    }

    private static HikariDataSource dataSource(String schema) {
        if (!schema.matches("it_[a-f0-9]{32}")) {
            throw new IllegalArgumentException("Schema de teste inválido");
        }
        var dataSource = new PostgresTestConfiguration().dataSource(postgres);
        dataSource.setSchema(schema);
        return dataSource;
    }

    private String novoSchema() {
        String schema = "it_" + UUID.randomUUID().toString().replace("-", "");
        try (var dataSource = new PostgresTestConfiguration().dataSource(postgres)) {
            new JdbcTemplate(dataSource).execute("CREATE SCHEMA " + schema);
        }
        return schema;
    }

    private void prepararLegado(String schema) {
        try (var dataSource = dataSource(schema)) {
            new ResourceDatabasePopulator(new ClassPathResource("db/migration/V1__criar_candidaturas.sql"))
                    .execute(dataSource);
            new JdbcTemplate(dataSource).update("""
                    INSERT INTO candidaturas (empresa, titulo, url_vaga, modalidade, status)
                    VALUES ('Legado', 'Java Júnior', 'https://example.com/vaga', 'HIBRIDO', 'INSCRITO')
                    """);
        }
    }

    private List<Map<String, Object>> registros(String schema) {
        try (var dataSource = dataSource(schema)) {
            return new JdbcTemplate(dataSource).queryForList("SELECT * FROM candidaturas ORDER BY id");
        }
    }

    private List<Map<String, Object>> historico(String schema) {
        try (var dataSource = dataSource(schema)) {
            return new JdbcTemplate(dataSource).queryForList("SELECT * FROM flyway_schema_history ORDER BY installed_rank");
        }
    }

    private void assertFalhaFlyway(Throwable falha) {
        assertNotNull(falha, "Contexto Spring deve falhar, impedindo a inicialização da aplicação");
        for (Throwable causa = falha; causa != null; causa = causa.getCause()) {
            if (causa instanceof FlywayException) return;
        }
        fail("A falha de inicialização deve ser causada pelo Flyway", falha);
    }

    private String mensagens(Throwable falha) {
        StringBuilder mensagens = new StringBuilder();
        for (Throwable causa = falha; causa != null; causa = causa.getCause()) {
            mensagens.append(causa.getMessage()).append('\n');
        }
        return mensagens.toString();
    }
}
