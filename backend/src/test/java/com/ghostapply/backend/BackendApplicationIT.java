package com.ghostapply.backend;

import org.junit.jupiter.api.Test;
import com.ghostapply.backend.support.PostgresIntegrationTest;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.*;

class BackendApplicationIT extends PostgresIntegrationTest {
    @Autowired private HikariDataSource dataSource;
    @Autowired private PostgreSQLContainer postgres;
    @Autowired private JdbcTemplate jdbc;

	@Test
	void contextLoads() {
		assertTrue(postgres.isRunning());
		assertEquals(postgres.getJdbcUrl(), dataSource.getJdbcUrl());
		assertEquals(postgres.getUsername(), dataSource.getUsername());
		assertEquals("ghostapply_it", jdbc.queryForObject("SELECT current_database()", String.class));
	}

}
