package com.vinicius.scene_api.idempotency;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Registro de uma chave de idempotência já usada em um POST (tabela {@code idempotency_keys}).
 * Guarda o hash da requisição original e a resposta devolvida, para repetir
 * essa mesma resposta se o cliente reenviar a chave.
 */
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Valor do header X-Idempotency-Key. Único: o banco impede duas requisições com a mesma chave. */
    @Column(nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    /** Operação em que a chave foi usada, por exemplo "POST /artists". */
    @Column(nullable = false, length = 150)
    private String operation;

    /** SHA-256 da operação e do corpo enviado: mostra se uma repetição é mesmo a mesma requisição. */
    @Column(nullable = false, length = 64)
    private String requestHash;

    /** Corpo (JSON) da resposta original. Fica nulo só enquanto a requisição ainda está sendo processada. */
    @Column(length = 8000)
    private String responseBody;

    @Column(nullable = false)
    private Instant createdAt;

    /** A partir deste momento a chave deixa de valer e pode ser usada de novo. */
    @Column(nullable = false)
    private Instant expiresAt;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(String idempotencyKey, String operation, String requestHash,
                             Instant createdAt, Instant expiresAt) {
        this.idempotencyKey = idempotencyKey;
        this.operation = operation;
        this.requestHash = requestHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getOperation() {
        return operation;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
