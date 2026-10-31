package com.ghostapply.backend;

import org.junit.jupiter.api.Test;
import com.ghostapply.backend.support.PostgresIntegrationTest;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.flywaydb.core.Flyway;
import static org.junit.jupiter.api.Assertions.*;

class BackendApplicationIT extends PostgresIntegrationTest {
    @Autowired private HikariDataSource dataSource;
    @Autowired private PostgreSQLContainer postgres;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private Flyway flyway;

	@Test
	void contextLoads() {
		assertTrue(postgres.isRunning());
		assertEquals(postgres.getJdbcUrl(), dataSource.getJdbcUrl());
		assertEquals(postgres.getUsername(), dataSource.getUsername());
		assertEquals("ghostapply_it", jdbc.queryForObject("SELECT current_database()", String.class));
		assertSame(dataSource, flyway.getConfiguration().getDataSource());
		assertFalse(flyway.getConfiguration().isBaselineOnMigrate());
		assertTrue(flyway.getConfiguration().isValidateOnMigrate());
		assertTrue(flyway.getConfiguration().isCleanDisabled());
		assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND type = 'SQL' AND success",
                Integer.class));
		assertEquals("V1__criar_candidaturas.sql", jdbc.queryForObject(
                "SELECT script FROM flyway_schema_history WHERE version = '1'", String.class));
		assertNotNull(jdbc.queryForObject(
                "SELECT checksum FROM flyway_schema_history WHERE version = '1'", Integer.class));
	}

}
