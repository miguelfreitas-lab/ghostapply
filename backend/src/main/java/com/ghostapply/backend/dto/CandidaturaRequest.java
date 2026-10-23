package com.ghostapply.backend.dto;

import com.ghostapply.backend.model.Modalidade;
import com.ghostapply.backend.model.StatusCandidatura;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;

public record CandidaturaRequest(
        @NotBlank(message = "Empresa é obrigatória") @Size(max = 200) String empresa,
        @NotBlank(message = "Título é obrigatório") @Size(max = 200) String titulo,
        @Size(max = 2048) @URL(message = "Informe uma URL válida")
        @Pattern(regexp = "^$|https?://.*", message = "Use uma URL HTTP ou HTTPS") String urlVaga,
        @NotNull(message = "Modalidade é obrigatória") Modalidade modalidade,
        @NotNull(message = "Status é obrigatório") StatusCandidatura status
) {}
