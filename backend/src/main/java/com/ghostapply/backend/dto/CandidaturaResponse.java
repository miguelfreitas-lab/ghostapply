package com.ghostapply.backend.dto;

import com.ghostapply.backend.model.*;
import java.time.Instant;

public record CandidaturaResponse(Long id, String empresa, String titulo, String urlVaga,
                                  Modalidade modalidade, StatusCandidatura status, Instant criadaEm) {
    public static CandidaturaResponse from(Candidatura candidatura) {
        return new CandidaturaResponse(candidatura.getId(), candidatura.getEmpresa(),
                candidatura.getTitulo(), candidatura.getUrlVaga(), candidatura.getModalidade(),
                candidatura.getStatus(), candidatura.getCriadaEm());
    }
}
