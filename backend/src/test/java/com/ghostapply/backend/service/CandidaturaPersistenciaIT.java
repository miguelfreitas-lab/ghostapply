package com.ghostapply.backend.service;

import com.ghostapply.backend.dto.CandidaturaRequest;
import com.ghostapply.backend.dto.CandidaturaResponse;
import com.ghostapply.backend.model.Modalidade;
import com.ghostapply.backend.model.StatusCandidatura;
import com.ghostapply.backend.repository.CandidaturaRepository;
import com.ghostapply.backend.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

// Sem @Transactional no teste: cada operação do service confirma sua transação.
// As consultas seguintes usam outro contexto JPA e o JDBC confirma o dado persistido.
class CandidaturaPersistenciaIT extends PostgresIntegrationTest {
    @Autowired private CandidaturaService service;
    @Autowired private CandidaturaRepository repository;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void cicloCompletoPersisteEReleDoPostgres() {
        Instant inicio = Instant.now().minusSeconds(1);
        CandidaturaResponse criada = service.criar(request(" Empresa ", " Java Júnior ", Modalidade.HIBRIDO, StatusCandidatura.INSCRITO));
        try {
            assertNotNull(criada.id());
            assertTrue(criada.id() > 0);
            CandidaturaResponse lida = service.buscar(criada.id());
            assertEquals("Empresa", lida.empresa());
            assertEquals("Java Júnior", lida.titulo());
            assertNull(lida.urlVaga());
            assertEquals(Modalidade.HIBRIDO, lida.modalidade());
            assertEquals(StatusCandidatura.INSCRITO, lida.status());
            assertNotNull(lida.criadaEm());
            assertFalse(lida.criadaEm().isBefore(inicio));
            assertFalse(lida.criadaEm().isAfter(Instant.now().plusSeconds(1)));
            assertEquals(lida.criadaEm(), jdbc.queryForObject(
                    "SELECT criada_em FROM candidaturas WHERE id = ?",
                    (rs, row) -> rs.getTimestamp(1).toInstant(), criada.id()));
            assertEquals("HIBRIDO", jdbc.queryForObject(
                    "SELECT modalidade FROM candidaturas WHERE id = ?", String.class, criada.id()));
            assertTrue(service.listar().stream().anyMatch(item -> item.id().equals(criada.id())));

            service.atualizar(criada.id(), new CandidaturaRequest("Atualizada", "Java Pleno",
                    "https://example.com/vaga", Modalidade.REMOTO, StatusCandidatura.OFERTA));
            CandidaturaResponse atualizada = service.buscar(criada.id());
            assertEquals(criada.id(), atualizada.id());
            assertEquals(lida.criadaEm(), atualizada.criadaEm());
            assertEquals("Atualizada", atualizada.empresa());
            assertEquals("Java Pleno", atualizada.titulo());
            assertEquals("https://example.com/vaga", atualizada.urlVaga());
            assertEquals(Modalidade.REMOTO, atualizada.modalidade());
            assertEquals(StatusCandidatura.OFERTA, atualizada.status());

            service.excluir(criada.id());
            assertFalse(repository.existsById(criada.id()));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM candidaturas WHERE id = ?", Integer.class, criada.id()));
            assertEquals(404, assertThrows(ResponseStatusException.class,
                    () -> service.buscar(criada.id())).getStatusCode().value());
        } finally {
            // Somente o registro criado por este teste, no banco descartável.
            repository.deleteById(criada.id());
        }
    }

    @ParameterizedTest
    @EnumSource(Modalidade.class)
    void persisteCadaModalidade(Modalidade modalidade) {
        CandidaturaResponse criada = service.criar(request("Empresa", "Vaga", modalidade, StatusCandidatura.SALVA));
        try {
            assertEquals(modalidade, service.buscar(criada.id()).modalidade());
            assertEquals(modalidade.name(), jdbc.queryForObject(
                    "SELECT modalidade FROM candidaturas WHERE id = ?", String.class, criada.id()));
        } finally {
            repository.deleteById(criada.id());
        }
    }

    @ParameterizedTest
    @EnumSource(StatusCandidatura.class)
    void persisteCadaStatus(StatusCandidatura status) {
        CandidaturaResponse criada = service.criar(request("Empresa", "Vaga", Modalidade.PRESENCIAL, status));
        try {
            assertEquals(status, service.buscar(criada.id()).status());
            assertEquals(status.name(), jdbc.queryForObject(
                    "SELECT status FROM candidaturas WHERE id = ?", String.class, criada.id()));
        } finally {
            repository.deleteById(criada.id());
        }
    }

    @Test
    void listaPorDataEIdDecrescentes() {
        CandidaturaResponse primeira = service.criar(request("Primeira", "Vaga", Modalidade.REMOTO, StatusCandidatura.SALVA));
        CandidaturaResponse segunda = service.criar(request("Segunda", "Vaga", Modalidade.HIBRIDO, StatusCandidatura.SALVA));
        try {
            Instant data = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            // Fixture de empate apenas no banco de testes; não altera a regra de criação.
            jdbc.update("UPDATE candidaturas SET criada_em = ? WHERE id = ? OR id = ?",
                    java.sql.Timestamp.from(data), primeira.id(), segunda.id());
            List<Long> ids = service.listar().stream()
                    .filter(item -> item.id().equals(primeira.id()) || item.id().equals(segunda.id()))
                    .map(CandidaturaResponse::id).toList();
            assertEquals(List.of(segunda.id(), primeira.id()), ids);

            jdbc.update("UPDATE candidaturas SET criada_em = ? WHERE id = ?",
                    java.sql.Timestamp.from(data.plusSeconds(1)), primeira.id());
            ids = service.listar().stream()
                    .filter(item -> item.id().equals(primeira.id()) || item.id().equals(segunda.id()))
                    .map(CandidaturaResponse::id).toList();
            assertEquals(List.of(primeira.id(), segunda.id()), ids);
        } finally {
            repository.deleteById(primeira.id());
            repository.deleteById(segunda.id());
        }
    }

    private CandidaturaRequest request(String empresa, String titulo, Modalidade modalidade, StatusCandidatura status) {
        return new CandidaturaRequest(empresa, titulo, null, modalidade, status);
    }
}
