package com.vinicius.scene_api.apikey;

import com.vinicius.scene_api.dto.ApiKeyCreatedResponse;
import com.vinicius.scene_api.dto.ApiKeyRequest;
import com.vinicius.scene_api.dto.ApiKeyResponse;
import com.vinicius.scene_api.exception.InvalidApiKeyException;
import com.vinicius.scene_api.validation.Text;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Gera, confere e revoga as chaves de API.
 * <p>
 * Cada chave é {@code scn_} seguido de 32 caracteres aleatórios (24 bytes do {@link SecureRandom}). O banco guarda
 * só o hash SHA-256 dela: para conferir uma chave recebida, a API calcula o hash e procura por ele.
 */
@Service
public class ApiKeyService {

    /** Header em que o cliente envia a chave. */
    public static final String HEADER = "X-API-Key";

    /** Nome do esquema de segurança no OpenAPI (é ele que faz o Swagger UI mostrar o botão Authorize). */
    public static final String SECURITY_SCHEME = "ApiKey";

    /** Valor do header WWW-Authenticate, que acompanha toda resposta 401 (diz ao cliente como se autenticar). */
    public static final String CHALLENGE = "ApiKey realm=\"Scene API\", header=\"" + HEADER + "\"";

    /** Atributo da requisição em que o interceptor deixa a chave já conferida, para o controller usar. */
    public static final String REQUEST_ATTRIBUTE = "com.vinicius.scene_api.apikey.CURRENT_KEY";

    public static final String MISSING_MESSAGE =
            "Esta rota exige uma chave de API no header X-API-Key. Gere uma em POST /api-keys.";
    public static final String UNKNOWN_MESSAGE =
            "Chave de API inválida: nenhuma chave cadastrada com esse valor. Confira o header X-API-Key.";
    public static final String REVOKED_MESSAGE =
            "Esta chave de API foi revogada. Gere uma nova em POST /api-keys.";

    /** Começo de toda chave: ajuda a reconhecer uma chave da Scene API num arquivo ou num log. */
    static final String KEY_START = "scn_";

    /** 24 bytes aleatórios (192 bits) viram 32 caracteres em Base64 próprio para URL. */
    private static final int RANDOM_BYTES = 24;

    /** Caracteres depois de "scn_" que ficam visíveis no prefixo (ex.: scn_q8Xv2LmA). */
    private static final int VISIBLE_CHARS = 8;

    private final ApiKeyRepository repository;
    private final SecureRandom random = new SecureRandom();

    public ApiKeyService(ApiKeyRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ApiKeyCreatedResponse create(ApiKeyRequest request) {
        byte[] bytes = new byte[RANDOM_BYTES];
        random.nextBytes(bytes);
        String key = KEY_START + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String prefix = key.substring(0, KEY_START.length() + VISIBLE_CHARS);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        ApiKey saved = repository.save(new ApiKey(Text.trim(request.owner()), hash(key), prefix, now));
        return ApiKeyCreatedResponse.from(saved, key);
    }

    /**
     * Confere a chave enviada no header. Sem chave, com uma que não existe ou com uma revogada, lança
     * {@link InvalidApiKeyException} (401), cada caso com a sua mensagem.
     */
    @Transactional(readOnly = true)
    public ApiKey authenticate(String key) {
        if (key == null || key.isBlank()) {
            throw new InvalidApiKeyException(MISSING_MESSAGE);
        }
        ApiKey found = repository.findByKeyHash(hash(key.trim()))
                .orElseThrow(() -> new InvalidApiKeyException(UNKNOWN_MESSAGE));
        if (!found.isActive()) {
            throw new InvalidApiKeyException(REVOKED_MESSAGE);
        }
        return found;
    }

    public ApiKeyResponse describe(ApiKey key) {
        return ApiKeyResponse.from(key);
    }

    /** Desativa a chave. Ela continua no banco (o hash), mas qualquer uso dela passa a devolver 401. */
    @Transactional
    public void revoke(ApiKey key) {
        repository.findById(key.getId()).ifPresent(ApiKey::revoke);
    }

    /** Hash SHA-256 da chave, em hexadecimal: é só isso que vai para o banco. */
    static String hash(String key) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("A JVM não tem SHA-256", ex);
        }
    }
}
