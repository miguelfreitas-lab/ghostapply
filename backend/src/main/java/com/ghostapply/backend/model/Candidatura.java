package com.ghostapply.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "candidaturas")
public class Candidatura {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String empresa;
    @Column(nullable = false, length = 200)
    private String titulo;
    @Column(name = "url_vaga", length = 2048)
    private String urlVaga;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Modalidade modalidade;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusCandidatura status;
    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    protected Candidatura() {}

    public Candidatura(String empresa, String titulo, String urlVaga,
                       Modalidade modalidade, StatusCandidatura status) {
        atualizar(empresa, titulo, urlVaga, modalidade, status);
    }

    @PrePersist
    void registrarCriacao() { criadaEm = Instant.now(); }

    public void atualizar(String empresa, String titulo, String urlVaga,
                          Modalidade modalidade, StatusCandidatura status) {
        this.empresa = empresa.strip();
        this.titulo = titulo.strip();
        this.urlVaga = urlVaga == null || urlVaga.isBlank() ? null : urlVaga.strip();
        this.modalidade = modalidade;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getEmpresa() { return empresa; }
    public String getTitulo() { return titulo; }
    public String getUrlVaga() { return urlVaga; }
    public Modalidade getModalidade() { return modalidade; }
    public StatusCandidatura getStatus() { return status; }
    public Instant getCriadaEm() { return criadaEm; }
}
