package com.ghostapply.backend.service;

import com.ghostapply.backend.dto.CandidaturaRequest;
import com.ghostapply.backend.model.*;
import com.ghostapply.backend.repository.CandidaturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import java.time.Instant;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CandidaturaServiceTest {
    private CandidaturaRepository repository;
    private CandidaturaService service;
    private final CandidaturaRequest request = new CandidaturaRequest(" Empresa ", " Java Júnior ",
            "", Modalidade.REMOTO, StatusCandidatura.SALVA);

    @BeforeEach
    void setup() {
        repository = mock(CandidaturaRepository.class);
        service = new CandidaturaService(repository);
    }

    @Test
    void criarNormalizaCamposEDeixaUrlVaziaNula() {
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.criar(request);
        assertEquals("Empresa", response.empresa());
        assertEquals("Java Júnior", response.titulo());
        assertNull(response.urlVaga());
        verify(repository).saveAndFlush(any(Candidatura.class));
    }

    @Test
    void atualizarPreservaIdEDataDeCriacao() {
        Candidatura existente = new Candidatura("Original", "Estágio", null, Modalidade.HIBRIDO, StatusCandidatura.SALVA);
        ReflectionTestUtils.setField(existente, "id", 1L);
        ReflectionTestUtils.setField(existente, "criadaEm", Instant.parse("2026-10-22T12:00:00Z"));
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.saveAndFlush(existente)).thenReturn(existente);
        var antes = service.buscar(1L);
        var depois = service.atualizar(1L, request);
        assertEquals("Empresa", depois.empresa());
        assertEquals(antes.id(), depois.id());
        assertEquals(antes.criadaEm(), depois.criadaEm());
    }

    @Test
    void excluirRemoveRegistroEncontrado() {
        Candidatura existente = new Candidatura("Empresa", "Java", null, Modalidade.REMOTO, StatusCandidatura.SALVA);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        service.excluir(1L);
        verify(repository).delete(existente);
    }

    @Test
    void operacoesComIdInexistenteRetornam404() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.buscar(99L)).getStatusCode().value());
        assertThrows(ResponseStatusException.class, () -> service.atualizar(99L, request));
        assertThrows(ResponseStatusException.class, () -> service.excluir(99L));
        verify(repository, never()).delete(any());
    }
}
