package com.ghostapply.backend.support;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.util.UUID;

@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfiguration {
    @Bean(initMethod = "start", destroyMethod = "stop")
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:18.0-alpine")
                .withDatabaseName("ghostapply_it")
                .withUsername("ghostapply_it")
                .withPassword(UUID.randomUUID().toString())
                .withReuse(false)
                .withInitScript("db/migration/V1__criar_candidaturas.sql");
    }

    @Bean(destroyMethod = "close")
    HikariDataSource dataSource(PostgreSQLContainer postgres) {
        // A dependência do bean garante start + SQL concluídos antes do DataSource.
        if (!postgres.isRunning()) {
            throw new IllegalStateException("PostgreSQL de testes indisponível; conexão alternativa proibida");
        }
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(postgres.getJdbcUrl());
        dataSource.setUsername(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setMaximumPoolSize(3);
        dataSource.setPoolName("ghostapply-testcontainers");
        return dataSource;
    }
}
