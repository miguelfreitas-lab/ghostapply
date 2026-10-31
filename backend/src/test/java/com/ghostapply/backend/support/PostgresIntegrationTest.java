package com.ghostapply.backend.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("integration")
@Import(PostgresTestConfiguration.class)
public abstract class PostgresIntegrationTest {}
