package com.vinicius.scene_api.idempotency;

import com.vinicius.scene_api.exception.BadRequestException;
import com.vinicius.scene_api.exception.ConflictException;
import com.vinicius.scene_api.exception.IdempotencyKeyReuseException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Idempotência das operações POST.
 * <p>
 * O cliente manda uma chave única no cabeçalho {@code X-Idempotency-Key}. Na primeira vez a operação roda
 * normalmente e a resposta fica guardada junto com a chave. Se a mesma requisição chegar de novo com a mesma
 * chave (por exemplo, o cliente não recebeu a resposta e tentou outra vez), a operação <b>não</b> roda de
 * novo: a API devolve a resposta guardada. Sem o cabeçalho, o POST funciona como sempre.
 */
@Service
public class IdempotencyService {

    /** Cabeçalho de requisição com a chave de idempotência. */
    public static final String HEADER = "X-Idempotency-Key";

    /** Cabeçalho de resposta: {@code true} quando a resposta é a original, guardada. */
    public static final String REPLAYED_HEADER = "X-Idempotent-Replayed";

    /** Formato aceito para a chave: de 1 a 100 letras, números, ponto, hífen, dois-pontos ou underline. */
    public static final String KEY_PATTERN = "^[A-Za-z0-9._:-]{1,100}$";

    private static final Pattern VALID_KEY = Pattern.compile(KEY_PATTERN);

    private final IdempotencyRecordRepository repository;
    private final IdempotencyProperties properties;
    private final JsonMapper jsonMapper;

    public IdempotencyService(IdempotencyRecordRepository repository, IdempotencyProperties properties,
                              JsonMapper jsonMapper) {
        this.repository = repository;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    /**
     * Executa a operação no máximo uma vez por chave.
     * <p>
     * A chave e o que a operação grava ficam na mesma transação: ou as duas coisas são salvas, ou nenhuma.
     * Se a operação falhar (400, 404, 409...), nada fica guardado e o cliente pode repetir com a mesma chave.
     *
     * @param key          valor do cabeçalho X-Idempotency-Key, ou {@code null} se o cliente não mandou
     * @param operation    operação protegida, por exemplo "POST /artists"
     * @param requestBody  corpo recebido (entra na impressão digital da requisição)
     * @param responseType tipo da resposta, para reconstruir a resposta guardada
     * @param action       a operação em si
     */
    @Transactional
    public <T> IdempotentResult<T> execute(String key, String operation, Object requestBody,
                                           Class<T> responseType, Supplier<T> action) {
        if (key == null) {
            return new IdempotentResult<>(action.get(), false, false);
        }
        if (!VALID_KEY.matcher(key).matches()) {
            throw new BadRequestException("Cabeçalho " + HEADER + " inválido: use de 1 a 100 caracteres entre letras, "
                    + "números, ponto, hífen, dois-pontos e underline (ex.: um UUID)");
        }

        String requestHash = hash(operation, requestBody);
        Instant now = Instant.now();
        repository.deleteByExpiresAtBefore(now);

        Optional<IdempotencyRecord> existing = repository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            IdempotencyRecord original = existing.get();
            if (!original.getRequestHash().equals(requestHash)) {
                throw new IdempotencyKeyReuseException("A chave de idempotência '" + key + "' já foi usada em outra "
                        + "requisição (" + original.getOperation() + "). Use uma chave nova para cada operação diferente.");
            }
            T originalResponse = jsonMapper.readValue(original.getResponseBody(), responseType);
            return new IdempotentResult<>(originalResponse, true, true);
        }

        IdempotencyRecord record = reserve(
                new IdempotencyRecord(key, operation, requestHash, now, now.plus(properties.ttl())));
        T response = action.get();
        record.setResponseBody(jsonMapper.writeValueAsString(response));
        return new IdempotentResult<>(response, true, false);
    }

    /**
     * Grava a chave antes de rodar a operação. A coluna é única, então, se duas requisições iguais chegarem
     * ao mesmo tempo, só uma consegue gravar; a outra recebe 409 e pode tentar de novo em seguida.
     */
    private IdempotencyRecord reserve(IdempotencyRecord record) {
        try {
            return repository.saveAndFlush(record);
        } catch (DataIntegrityViolationException | ConcurrencyFailureException ex) {
            throw new ConflictException("Já existe uma requisição em andamento com a chave de idempotência '"
                    + record.getIdempotencyKey() + "'. Aguarde e tente de novo.");
        }
    }

    /** Impressão digital da requisição: SHA-256 da operação e do corpo. */
    private String hash(String operation, Object requestBody) {
        String content = operation + "\n" + jsonMapper.writeValueAsString(requestBody);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível", ex);
        }
    }
}
