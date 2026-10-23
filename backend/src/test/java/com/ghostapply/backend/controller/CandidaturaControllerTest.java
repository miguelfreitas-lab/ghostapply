package com.ghostapply.backend.controller;

import com.ghostapply.backend.dto.*;
import com.ghostapply.backend.model.*;
import com.ghostapply.backend.service.CandidaturaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CandidaturaControllerTest {
    private CandidaturaService service;
    private MockMvc mvc;
    private final String body = """
            {"empresa":"Ghost","titulo":"Java","urlVaga":"https://example.com/vaga",
             "modalidade":"REMOTO","status":"SALVA"}
            """;
    private final CandidaturaResponse response = new CandidaturaResponse(1L, "Ghost", "Java",
            "https://example.com/vaga", Modalidade.REMOTO, StatusCandidatura.SALVA, Instant.parse("2026-10-22T12:00:00Z"));

    @BeforeEach
    void setup() {
        service = mock(CandidaturaService.class);
        mvc = MockMvcBuilders.standaloneSetup(new CandidaturaController(service), new HealthController())
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void criarRetorna201ELocation() throws Exception {
        when(service.criar(any())).thenReturn(response);
        mvc.perform(post("/api/candidaturas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/candidaturas/1"))
                .andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.criadaEm").exists());
    }

    @Test
    void listarBuscarAtualizarEExcluir() throws Exception {
        when(service.listar()).thenReturn(List.of(response));
        when(service.buscar(1L)).thenReturn(response);
        when(service.atualizar(eq(1L), any())).thenReturn(response);
        mvc.perform(get("/api/candidaturas")).andExpect(status().isOk()).andExpect(jsonPath("$[0].empresa").value("Ghost"));
        mvc.perform(get("/api/candidaturas/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        mvc.perform(put("/api/candidaturas/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SALVA"));
        mvc.perform(delete("/api/candidaturas/1")).andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).excluir(1L);
    }

    @Test
    void dadosInvalidosRetornam400SemChamarService() throws Exception {
        for (String invalid : List.of(body.replace("Ghost", " "), body.replace("Java", ""),
                body.replace("REMOTO", "INVALIDO"), body.replace("\"SALVA\"", "null"),
                body.replace("https://example.com/vaga", "javascript:alert(1)"), "{}", "{")) {
            mvc.perform(post("/api/candidaturas").contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        }
        mvc.perform(put("/api/candidaturas/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void idAusenteRetorna404() throws Exception {
        when(service.buscar(99L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidatura não encontrada"));
        mvc.perform(get("/api/candidaturas/99")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Candidatura não encontrada"));
    }

    @Test
    void preservaHealth() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP")).andExpect(jsonPath("$.application").value("GhostApply"));
    }
}
