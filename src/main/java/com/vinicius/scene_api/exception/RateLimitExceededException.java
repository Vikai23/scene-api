package com.vinicius.scene_api.exception;

/**
 * O cliente esgotou os tokens do rate limiting: vira 429 Too Many Requests com o header Retry-After.
 */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Limite de requisições excedido. Tente novamente em " + retryAfterSeconds + " segundo(s).");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /** Segundos que o cliente deve esperar antes de tentar de novo. */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
