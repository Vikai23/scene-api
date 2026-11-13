package com.vinicius.scene_api.apikey;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    /** A busca é pelo hash: a chave enviada no header passa pelo mesmo SHA-256 antes de consultar o banco. */
    Optional<ApiKey> findByKeyHash(String keyHash);
}
