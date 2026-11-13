package com.vinicius.scene_api.apikey;

import com.vinicius.scene_api.validation.ValidationRules;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Chave de API de um usuário.
 * <p>
 * A chave em si não fica no banco: só o hash SHA-256 dela. Por isso o valor completo aparece uma única vez, na
 * resposta de POST /api-keys. Para reconhecer a chave depois (sem expor o valor), fica guardado o começo dela
 * ({@code prefix}, ex.: scn_Xy12Ab34).
 */
@Entity
@Table(name = "api_keys")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nome de quem usa a chave. */
    @NotBlank
    @Size(min = 2, max = 100)
    @Pattern(regexp = ValidationRules.NAME, message = ValidationRules.NAME_MESSAGE)
    @Column(nullable = false, length = 100)
    private String owner;

    /** Hash SHA-256 da chave, em hexadecimal (64 caracteres). */
    @NotBlank
    @Size(min = 64, max = 64)
    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    private String keyHash;

    /** Começo da chave, para identificá-la sem mostrar o valor inteiro. */
    @NotBlank
    @Size(max = 16)
    @Column(nullable = false, length = 16)
    private String prefix;

    /** Falso depois que a chave é revogada: a partir daí ela não abre mais as rotas protegidas. */
    @Column(nullable = false)
    private boolean active = true;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ApiKey() {
    }

    public ApiKey(String owner, String keyHash, String prefix, LocalDateTime createdAt) {
        this.owner = owner;
        this.keyHash = keyHash;
        this.prefix = prefix;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void revoke() {
        this.active = false;
    }
}
