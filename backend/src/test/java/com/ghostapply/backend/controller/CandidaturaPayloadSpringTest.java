package com.ghostapply.backend.controller;

import com.ghostapply.backend.model.Candidatura;
import com.ghostapply.backend.model.Modalidade;
import com.ghostapply.backend.model.StatusCandidatura;
import com.ghostapply.backend.repository.CandidaturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Usa a configuração MVC/Jackson e a validação do Spring; somente a persistência é simulada.
@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
@AutoConfigureMockMvc
class CandidaturaPayloadSpringTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private CandidaturaRepository repository;
    private final String payload = """
            {
              "empresa": "Huawei",
              "titulo": "Desenvolvedor Cloud Júnior",
              "modalidade": "HIBRIDO",
              "status": "INSCRITO",
              "urlVaga": null
            }
            """;

    @BeforeEach
    void setup() {
        when(repository.saveAndFlush(any(Candidatura.class))).thenAnswer(invocation -> {
            Candidatura candidatura = invocation.getArgument(0);
            ReflectionTestUtils.setField(candidatura, "id", 1L);
            ReflectionTestUtils.setField(candidatura, "criadaEm", Instant.parse("2026-10-22T12:00:00Z"));
            return candidatura;
        });
    }

    @Test
    void payloadExatoEValidoComConfiguracaoSpring() throws Exception {
        mvc.perform(post("/api/candidaturas").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.empresa").value("Huawei"))
                .andExpect(jsonPath("$.titulo").value("Desenvolvedor Cloud Júnior"))
                .andExpect(jsonPath("$.modalidade").value("HIBRIDO"))
                .andExpect(jsonPath("$.status").value("INSCRITO"));
        verify(repository).saveAndFlush(argThat(candidatura ->
                candidatura.getModalidade() == Modalidade.HIBRIDO
                        && candidatura.getStatus() == StatusCandidatura.INSCRITO
                        && candidatura.getUrlVaga() == null));
    }

    @Test
    void mensagemGenericaOcultaDetalhesDaFalhaDeDesserializacao() throws Exception {
        var result = mvc.perform(post("/api/candidaturas").contentType(MediaType.APPLICATION_JSON)
                        .content(payload.replace("HIBRIDO", "INVALIDO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados inválidos. Verifique os campos e os valores de modalidade e status."))
                .andReturn();
        var exception = assertInstanceOf(HttpMessageNotReadableException.class, result.getResolvedException());
        assertNotNull(exception.getCause());
        assertTrue(exception.getMostSpecificCause().getMessage().contains("INVALIDO"));
        verifyNoInteractions(repository);
    }
}
