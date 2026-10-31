package com.ghostapply.backend.support;

import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostgresTestConfigurationTest {
    @Test
    void impedeCriacaoDoDatasourceSeContainerNaoEstaRodando() {
        PostgreSQLContainer postgres = mock(PostgreSQLContainer.class);
        assertThrows(IllegalStateException.class,
                () -> new PostgresTestConfiguration().dataSource(postgres));
        verify(postgres, never()).getJdbcUrl();
    }

    @Test
    void datasourceRecebeSomenteDadosDoContainerSemAbrirConexao() {
        PostgreSQLContainer postgres = mock(PostgreSQLContainer.class);
        when(postgres.isRunning()).thenReturn(true);
        when(postgres.getJdbcUrl()).thenReturn("jdbc:postgresql://127.0.0.1:65000/ghostapply_it");
        when(postgres.getUsername()).thenReturn("usuario_temporario");
        when(postgres.getPassword()).thenReturn("senha_temporaria");
        try (var dataSource = new PostgresTestConfiguration().dataSource(postgres)) {
            assertEquals(postgres.getJdbcUrl(), dataSource.getJdbcUrl());
            assertEquals(postgres.getUsername(), dataSource.getUsername());
            assertEquals(postgres.getPassword(), dataSource.getPassword());
            assertNull(dataSource.getHikariPoolMXBean(), "O pool não deve ter aberto conexão neste teste rápido");
        }
    }
}
