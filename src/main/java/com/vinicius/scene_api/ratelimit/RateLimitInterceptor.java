package com.vinicius.scene_api.ratelimit;

import com.vinicius.scene_api.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Roda antes de cada rota da API: consome um token do bucket do IP que fez a chamada e informa o saldo nos
 * headers {@code X-RateLimit-*}. Sem token, lança {@link RateLimitExceededException}, que vira
 * 429 Too Many Requests com o header {@code Retry-After} no tratamento global de erros.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    /** Capacidade do bucket (maior burst possível). */
    public static final String LIMIT_HEADER = "X-RateLimit-Limit";

    /** Tokens que sobraram depois desta requisição. */
    public static final String REMAINING_HEADER = "X-RateLimit-Remaining";

    /** Segundos até o bucket ficar cheio de novo. */
    public static final String RESET_HEADER = "X-RateLimit-Reset";

    private final RateLimiter rateLimiter;

    public RateLimitInterceptor(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        RateLimitResult result = rateLimiter.tryConsume(request.getRemoteAddr());
        response.setHeader(LIMIT_HEADER, String.valueOf(result.limit()));
        response.setHeader(REMAINING_HEADER, String.valueOf(result.remaining()));
        response.setHeader(RESET_HEADER, String.valueOf(result.resetSeconds()));
        if (!result.allowed()) {
            throw new RateLimitExceededException(result.retryAfterSeconds());
        }
        return true;
    }
}
