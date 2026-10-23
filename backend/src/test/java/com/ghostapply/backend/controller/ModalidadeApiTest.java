package com.ghostapply.backend.controller;

import com.ghostapply.backend.model.*;
import com.ghostapply.backend.repository.CandidaturaRepository;
import com.ghostapply.backend.service.CandidaturaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Exercita JSON, validação, controller, service, entidade e DTO de saída sem acessar o banco.
class ModalidadeApiTest {
    private CandidaturaRepository repository;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        repository = mock(CandidaturaRepository.class);
        when(repository.saveAndFlush(any(Candidatura.class))).thenAnswer(invocation -> {
            Candidatura candidatura = invocation.getArgument(0);
            if (candidatura.getId() == null) {
                ReflectionTestUtils.setField(candidatura, "id", 1L);
                ReflectionTestUtils.setField(candidatura, "criadaEm", Instant.parse("2026-10-22T12:00:00Z"));
            }
            return candidatura;
        });
        mvc = MockMvcBuilders.standaloneSetup(
                new CandidaturaController(new CandidaturaService(repository)))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @ParameterizedTest
    @EnumSource(Modalidade.class)
    void cadastrarAceitaEPreservaCadaModalidade(Modalidade modalidade) throws Exception {
        mvc.perform(post("/api/candidaturas").contentType(MediaType.APPLICATION_JSON)
                        .content(body(modalidade)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/candidaturas/1"))
                .andExpect(jsonPath("$.modalidade").value(modalidade.name()));

        verify(repository).saveAndFlush(argThat(candidatura -> candidatura.getModalidade() == modalidade));
    }

    @ParameterizedTest
    @EnumSource(Modalidade.class)
    void editarAceitaEPreservaCadaModalidade(Modalidade modalidade) throws Exception {
        Candidatura existente = new Candidatura("Empresa", "Java", null, Modalidade.REMOTO, StatusCandidatura.SALVA);
        ReflectionTestUtils.setField(existente, "id", 1L);
        Instant criadaEm = Instant.parse("2026-10-22T12:00:00Z");
        ReflectionTestUtils.setField(existente, "criadaEm", criadaEm);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        mvc.perform(put("/api/candidaturas/1").contentType(MediaType.APPLICATION_JSON)
                        .content(body(modalidade)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modalidade").value(modalidade.name()));

        assertEquals(modalidade, existente.getModalidade());
        assertEquals(criadaEm, existente.getCriadaEm());
        verify(repository).saveAndFlush(existente);
    }

    private String body(Modalidade modalidade) {
        return """
                {"empresa":"Empresa","titulo":"Java","urlVaga":null,
                 "modalidade":"%s","status":"SALVA"}
                """.formatted(modalidade.name());
    }
}
