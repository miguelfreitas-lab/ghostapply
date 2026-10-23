package com.ghostapply.backend.controller;

import com.ghostapply.backend.dto.*;
import com.ghostapply.backend.service.CandidaturaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/candidaturas")
public class CandidaturaController {
    private final CandidaturaService service;
    public CandidaturaController(CandidaturaService service) { this.service = service; }

    @GetMapping
    public List<CandidaturaResponse> listar() { return service.listar(); }

    @GetMapping("/{id}")
    public CandidaturaResponse buscar(@PathVariable Long id) { return service.buscar(id); }

    @PostMapping
    public ResponseEntity<CandidaturaResponse> criar(@Valid @RequestBody CandidaturaRequest request) {
        CandidaturaResponse response = service.criar(request);
        return ResponseEntity.created(URI.create("/api/candidaturas/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public CandidaturaResponse atualizar(@PathVariable Long id, @Valid @RequestBody CandidaturaRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
