package com.vinicius.scene_api.exception;

import com.vinicius.scene_api.config.ApiVersionConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.apache.tomcat.util.http.InvalidParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.InputCoercionException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Centraliza o tratamento de erros: toda falha vira um {@link ApiError} com o status HTTP correto.
 * A ideia é que nenhuma entrada inválida (texto no lugar de número, id inexistente, JSON quebrado,
 * campo de ordenação errado, versão da API inexistente...) chegue ao cliente como 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- 404 / 409 / 400 / 422 / 429 lançados pela própria API ----------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    /** Chave de idempotência reaproveitada em uma requisição diferente da original. */
    @ExceptionHandler(IdempotencyKeyReuseException.class)
    public ResponseEntity<ApiError> handleIdempotencyKeyReuse(IdempotencyKeyReuseException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), request, List.of());
    }

    /** Rate limiting: o cliente gastou todas as fichas. O cabeçalho Retry-After diz quanto esperar. */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiError> handleRateLimit(RateLimitExceededException ex, HttpServletRequest request) {
        ApiError body = new ApiError(HttpStatus.TOO_MANY_REQUESTS.value(), HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                ex.getMessage(), request.getRequestURI(), LocalDateTime.now(), List.of());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    // ---------- 400: validação e entrada inválida ----------

    /** Corpo (JSON) com campos inválidos segundo o Bean Validation. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldIssue> details = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> details.add(new ApiError.FieldIssue(error.getField(), error.getDefaultMessage())));
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> details.add(new ApiError.FieldIssue(error.getObjectName(), error.getDefaultMessage())));
        details.sort(Comparator.comparing(ApiError.FieldIssue::field));
        String message = "Dados inválidos: " + details.size() + " campo(s) com problema";
        return build(HttpStatus.BAD_REQUEST, message, request, details);
    }

    /** Parâmetros de rota/consulta com restrições (@NotBlank, @Size...) violadas. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleParameterValidation(HandlerMethodValidationException ex, HttpServletRequest request) {
        List<ApiError.FieldIssue> details = new ArrayList<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            String parameter = result.getMethodParameter().getParameterName();
            if (parameter == null) {
                parameter = "parâmetro";
            }
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                String field = error instanceof FieldError fieldError ? fieldError.getField() : parameter;
                details.add(new ApiError.FieldIssue(field, error.getDefaultMessage()));
            }
        }
        return build(HttpStatus.BAD_REQUEST, "Parâmetro inválido", request, details);
    }

    /** Violação de restrição lançada diretamente pelo Bean Validation (ex.: ao salvar a entidade). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<ApiError.FieldIssue> details = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String path = violation.getPropertyPath().toString();
            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            details.add(new ApiError.FieldIssue(field, violation.getMessage()));
        }
        details.sort(Comparator.comparing(ApiError.FieldIssue::field));
        String message = "Dados inválidos: " + details.size() + " campo(s) com problema";
        return build(HttpStatus.BAD_REQUEST, message, request, details);
    }

    /**
     * Corpo que não pôde ser lido. Os casos, do mais específico para o mais geral: corpo ausente; campo com
     * valor de tipo errado (texto onde se espera número, número onde se espera texto, enum inexistente...),
     * em que a resposta aponta o campo; número grande demais para o campo; e JSON malformado (faltando
     * aspas, chaves ou vírgulas).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        if (ex.getCause() == null) {
            return build(HttpStatus.BAD_REQUEST, "Corpo da requisição ausente: envie os dados em JSON", request, List.of());
        }
        JacksonException jsonError = findCause(ex, JacksonException.class);
        String field = jsonError == null ? null : lastFieldName(jsonError);
        if (field != null && jsonError instanceof MismatchedInputException wrongType) {
            return wrongTypeResponse(field, expectedValue(wrongType.getTargetType()), request);
        }
        if (field != null && jsonError instanceof InputCoercionException) {
            return wrongTypeResponse(field, "número grande demais para este campo", request);
        }
        if (jsonError instanceof MismatchedInputException) {
            return build(HttpStatus.BAD_REQUEST, "O corpo da requisição deve ser um objeto JSON, entre chaves", request, List.of());
        }
        return build(HttpStatus.BAD_REQUEST, "JSON malformado: confira se as aspas, vírgulas e chaves estão no lugar",
                request, List.of());
    }

    private static ResponseEntity<ApiError> wrongTypeResponse(String field, String problem, HttpServletRequest request) {
        List<ApiError.FieldIssue> details = List.of(new ApiError.FieldIssue(field, problem));
        return build(HttpStatus.BAD_REQUEST, "JSON com valor de tipo inválido: 1 campo(s) com problema", request, details);
    }

    /** Id não numérico na rota, enum inexistente no path, etc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        StringBuilder message = new StringBuilder("Parâmetro '")
                .append(ex.getName())
                .append("' com valor inválido: ")
                .append(ex.getValue())
                .append('.');
        Class<?> requiredType = ex.getRequiredType();
        if (requiredType != null && requiredType.isEnum()) {
            message.append(" Valores aceitos: ")
                    .append(Arrays.toString(requiredType.getEnumConstants()))
                    .append('.');
        }
        return build(HttpStatus.BAD_REQUEST, message.toString(), request, List.of());
    }

    /** Cabeçalho X-API-Version com uma versão que a API não tem (ou que nem é um número de versão). */
    @ExceptionHandler(InvalidApiVersionException.class)
    public ResponseEntity<ApiError> handleInvalidApiVersion(InvalidApiVersionException ex, HttpServletRequest request) {
        String message = "Versão da API não suportada: '" + request.getHeader(ApiVersionConfig.HEADER) + "'. "
                + "Use o cabeçalho " + ApiVersionConfig.HEADER + " com " + ApiVersionConfig.V1 + " ou " + ApiVersionConfig.V2
                + " (sem o cabeçalho vale a versão " + ApiVersionConfig.V1 + ").";
        return build(HttpStatus.BAD_REQUEST, message, request, List.of());
    }

    /** Parâmetro da URL com a codificação quebrada, como um % solto em ?name=% (o certo seria %25). */
    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<ApiError> handleInvalidUrlEncoding(InvalidParameterException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "Parâmetro da URL com codificação inválida: o caractere % deve ser enviado como %25", request, List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente: " + ex.getParameterName(), request, List.of());
    }

    /** Campo inexistente no parâmetro sort (ex.: sort=foo). */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiError> handleInvalidSort(PropertyReferenceException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Campo de ordenação inexistente: " + ex.getPropertyName(), request, List.of());
    }

    /**
     * Paginação ou ordenação que o banco não consegue executar, por exemplo página x tamanho acima do limite de
     * inteiro (page=999999999). Quando a requisição traz page, size ou sort, o erro é do cliente e vira 400;
     * nos demais casos segue o caminho de sempre.
     */
    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ApiError> handleInvalidDataAccess(InvalidDataAccessApiUsageException ex, HttpServletRequest request) {
        boolean pagedRequest = request.getParameter("page") != null
                || request.getParameter("size") != null
                || request.getParameter("sort") != null;
        if (!pagedRequest) {
            return handleAny(ex, request);
        }
        String reason = ex.getMostSpecificCause().getMessage();
        log.warn("Paginação/ordenação rejeitada em {} {}: {}", request.getMethod(), request.getRequestURI(), reason);
        PropertyReferenceException invalidSort = findCause(ex, PropertyReferenceException.class);
        if (invalidSort != null) {
            return handleInvalidSort(invalidSort, request);
        }
        if (reason != null && reason.contains("offset")) {
            return build(HttpStatus.BAD_REQUEST,
                    "Número de página muito grande para o tamanho informado (página x tamanho passa de "
                            + Integer.MAX_VALUE + ")", request, List.of());
        }
        return build(HttpStatus.BAD_REQUEST, "Parâmetros de paginação ou ordenação inválidos", request, List.of());
    }

    // ---------- 409: integridade do banco ----------

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violação de integridade em {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT,
                "Operação viola uma regra de integridade (valor duplicado ou registro em uso)", request, List.of());
    }

    /** Duas requisições disputaram o mesmo registro ao mesmo tempo e esta perdeu a vez (bloqueio no banco). */
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ApiError> handleConcurrency(ConcurrencyFailureException ex, HttpServletRequest request) {
        log.warn("Conflito de concorrência em {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT,
                "Outra requisição estava alterando o mesmo registro. Tente novamente.", request, List.of());
    }

    // ---------- Rede de segurança ----------

    /**
     * Qualquer outra exceção. Erros do próprio Spring MVC (rota inexistente, método não permitido,
     * Content-Type não suportado...) implementam {@link ErrorResponse} e mantêm o status original;
     * o que sobrar vira 500 com mensagem genérica, sem expor detalhes internos.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAny(Exception ex, HttpServletRequest request) {
        PropertyReferenceException invalidSort = findCause(ex, PropertyReferenceException.class);
        if (invalidSort != null) {
            return handleInvalidSort(invalidSort, request);
        }
        ConstraintViolationException violation = findCause(ex, ConstraintViolationException.class);
        if (violation != null) {
            return handleConstraintViolation(violation, request);
        }

        // Content-Type com curinga (ex.: application/*) não diz qual é o formato do corpo. O Spring acusa isso
        // com IllegalArgumentException, que aqui vira 415 em vez de cair no 500
        if (ex instanceof IllegalArgumentException && request.getContentType() != null && request.getContentType().contains("*")) {
            return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Content-Type não suportado: envie o corpo como application/json", request, List.of());
        }

        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode code = errorResponse.getStatusCode();
            HttpStatus status = HttpStatus.resolve(code.value());
            String reason = status != null ? status.getReasonPhrase() : "Error";
            ApiError body = new ApiError(code.value(), reason, springMessage(ex, errorResponse, status, request),
                    request.getRequestURI(), LocalDateTime.now(), List.of());
            return ResponseEntity.status(code)
                    .headers(errorResponse.getHeaders())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
        }

        log.error("Erro inesperado em {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno inesperado. Tente novamente mais tarde.", request, List.of());
    }

    // ---------- Auxiliares ----------

    private static String springMessage(Exception ex, ErrorResponse errorResponse, HttpStatus status, HttpServletRequest request) {
        if (status == HttpStatus.NOT_FOUND) {
            return "Rota inexistente: " + request.getRequestURI();
        }
        if (status == HttpStatus.METHOD_NOT_ALLOWED) {
            String method = ex instanceof HttpRequestMethodNotSupportedException notSupported
                    ? notSupported.getMethod() : request.getMethod();
            return "Método " + method + " não é suportado nesta rota";
        }
        if (status == HttpStatus.UNSUPPORTED_MEDIA_TYPE) {
            return "Content-Type não suportado: envie o corpo como application/json";
        }
        if (status == HttpStatus.NOT_ACCEPTABLE) {
            return "Formato de resposta não suportado: use Accept: application/json";
        }
        String detail = errorResponse.getBody().getDetail();
        if (detail != null && !detail.isBlank()) {
            return detail;
        }
        return status != null ? status.getReasonPhrase() : "Requisição não pôde ser processada";
    }

    /** Nome do campo em que o Jackson parou de ler (o último do caminho), ou null se o erro não está em um campo. */
    private static String lastFieldName(JacksonException ex) {
        String field = null;
        for (JacksonException.Reference step : ex.getPath()) {
            if (step.getPropertyName() != null) {
                field = step.getPropertyName();
            }
        }
        return field;
    }

    /** Explica, em português, que tipo de valor o campo aceita. */
    private static String expectedValue(Class<?> type) {
        if (type == null) {
            return "valor de tipo inválido";
        }
        if (type.isEnum()) {
            return "deve ser um destes valores (texto entre aspas): " + Arrays.toString(type.getEnumConstants());
        }
        if (type == String.class) {
            return "deve ser um texto entre aspas";
        }
        if (Number.class.isAssignableFrom(type)) {
            return "deve ser um número inteiro, sem aspas e sem casas decimais";
        }
        return "valor de tipo inválido";
    }

    private static <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth < 10) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
            depth++;
        }
        return null;
    }

    private static ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request,
                                                  List<ApiError.FieldIssue> details) {
        ApiError body = new ApiError(status.value(), status.getReasonPhrase(), message,
                request.getRequestURI(), LocalDateTime.now(), details);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
