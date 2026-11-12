package com.vinicius.scene_api.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    /** Apaga as chaves vencidas. */
    long deleteByExpiresAtBefore(Instant moment);
}
