package com.vinicius.scene_api.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Centraliza o tratamento de erros: toda falha vira um {@link ApiError} com o status HTTP correto.
 * A ideia é que nenhuma entrada inválida (texto no lugar de número, id inexistente, JSON quebrado,
 * campo de ordenação errado...) chegue ao cliente como 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- 404 / 409 / 400 lançados pela própria API ----------

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

    /** JSON malformado, corpo ausente, texto onde se espera número ou valor de enum inexistente. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message = "JSON malformado ou com valor inválido "
                + "(ex.: texto onde se espera número, ou valor de enum inexistente)";
        return build(HttpStatus.BAD_REQUEST, message, request, List.of());
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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente: " + ex.getParameterName(), request, List.of());
    }

    /** Campo inexistente no parâmetro sort (ex.: sort=foo). */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiError> handleInvalidSort(PropertyReferenceException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Campo de ordenação inexistente: " + ex.getPropertyName(), request, List.of());
    }

    // ---------- 409: integridade do banco ----------

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violação de integridade em {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT,
                "Operação viola uma regra de integridade (valor duplicado ou registro em uso)", request, List.of());
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
