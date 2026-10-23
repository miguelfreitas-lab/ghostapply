package com.ghostapply.backend.service;

import com.ghostapply.backend.dto.*;
import com.ghostapply.backend.model.Candidatura;
import com.ghostapply.backend.repository.CandidaturaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;

@Service
@Validated
@Transactional
public class CandidaturaService {
    private final CandidaturaRepository repository;

    public CandidaturaService(CandidaturaRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<CandidaturaResponse> listar() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "criadaEm", "id"))
                .stream().map(CandidaturaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CandidaturaResponse buscar(Long id) { return CandidaturaResponse.from(encontrar(id)); }

    public CandidaturaResponse criar(@Valid CandidaturaRequest request) {
        return CandidaturaResponse.from(repository.saveAndFlush(new Candidatura(request.empresa(),
                request.titulo(), request.urlVaga(), request.modalidade(), request.status())));
    }

    public CandidaturaResponse atualizar(Long id, @Valid CandidaturaRequest request) {
        Candidatura candidatura = encontrar(id);
        candidatura.atualizar(request.empresa(), request.titulo(), request.urlVaga(),
                request.modalidade(), request.status());
        return CandidaturaResponse.from(repository.saveAndFlush(candidatura));
    }

    public void excluir(Long id) { repository.delete(encontrar(id)); }

    private Candidatura encontrar(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidatura não encontrada"));
    }
}
