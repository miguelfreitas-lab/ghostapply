package com.ghostapply.backend.controller;

import com.ghostapply.backend.model.Candidatura;
import com.ghostapply.backend.model.Modalidade;
import com.ghostapply.backend.model.StatusCandidatura;
import com.ghostapply.backend.repository.CandidaturaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Diagnóstico explícito: requer o PostgreSQL configurado e a tabela existente.
 * Executar com -Dtest=CandidaturaDiagnosticoPostgresIT. Não cria nem altera dados.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureMockMvc
@Transactional(readOnly = true)
class CandidaturaDiagnosticoPostgresIT {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @MockitoSpyBean private CandidaturaRepository repository;

    @Test
    void payloadExatoChegaAoRepositorySemGravar() throws Exception {
        // Interrompe ANTES de qualquer INSERT, mesmo que o payload seja válido.
        doAnswer(invocation -> {
            Candidatura candidatura = invocation.getArgument(0);
            assertEquals("Huawei", candidatura.getEmpresa());
            assertEquals("Desenvolvedor Cloud Júnior", candidatura.getTitulo());
            assertEquals(Modalidade.HIBRIDO, candidatura.getModalidade());
            assertEquals(StatusCandidatura.INSCRITO, candidatura.getStatus());
            assertNull(candidatura.getUrlVaga());
            throw new DataAccessResourceFailureException("Interrupção intencional antes do INSERT");
        }).when(repository).saveAndFlush(any(Candidatura.class));

        mvc.perform(post("/api/candidaturas").contentType(MediaType.APPLICATION_JSON).content("""
                {
                  "empresa": "Huawei",
                  "titulo": "Desenvolvedor Cloud Júnior",
                  "modalidade": "HIBRIDO",
                  "status": "INSCRITO",
                  "urlVaga": null
                }
                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Não foi possível concluir a operação no banco de dados."));
        verify(repository).saveAndFlush(any(Candidatura.class));
    }

    @Test
    void listagemJpaCorrespondeAoTotalNoPostgres() throws Exception {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM candidaturas", Integer.class);
        mvc.perform(get("/api/candidaturas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(total));
    }

    @Test
    void colunaModalidadeETextualECheckAceitaHibrido() {
        String tipo = jdbc.queryForObject("""
                SELECT format_type(atttypid, atttypmod)
                FROM pg_attribute
                WHERE attrelid = 'candidaturas'::regclass AND attname = 'modalidade'
                """, String.class);
        assertEquals("character varying(20)", tipo);
        List<String> checks = jdbc.queryForList("""
                SELECT pg_get_constraintdef(oid)
                FROM pg_constraint
                WHERE conrelid = 'candidaturas'::regclass AND contype = 'c'
                """, String.class);
        assertTrue(checks.stream().anyMatch(check -> check.contains("modalidade")
                && check.contains("'REMOTO'") && check.contains("'HIBRIDO'")
                && check.contains("'PRESENCIAL'")), "O CHECK de modalidade deve aceitar os três valores");
    }
}
