package com.polo42.project_flow.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tratamento central de erros. Estende ResponseEntityExceptionHandler para que as excecoes do
 * proprio framework (JSON malformado, UUID invalido, rota inexistente, metodo nao suportado...)
 * mantenham o status HTTP correto (400/404/405/415) em vez de virarem 500.
 * Todo corpo de erro traz o campo "message".
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<Object> build(HttpStatusCode status, String message, String error, WebRequest request) {
        ErrorDetails details = new ErrorDetails(
            LocalDateTime.now(), message, request.getDescription(false), status.value(), error);
        return new ResponseEntity<>(details, status);
    }

    // 🔹 Recurso inexistente (ou pertencente a outro usuario)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), "Recurso não encontrado", request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequestException(BadRequestException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), "Requisição inválida", request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorizedException(UnauthorizedException ex, WebRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), "Não autorizado", request);
    }

    // 🔹 Regra de negocio
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusinessException(BusinessException ex, WebRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), "Erro de negócio", request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Email ou senha inválidos", "Credenciais inválidas", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        return build(HttpStatus.FORBIDDEN, "Acesso negado", "Permissão insuficiente", request);
    }

    // 🔹 Validacao de DTOs (@Valid): mantem um campo por erro e acrescenta "message"
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
            errors.putIfAbsent(field, error.getDefaultMessage());
        });
        Map<String, String> body = new LinkedHashMap<>(errors);
        body.put("message", errors.values().stream().collect(Collectors.joining("; ")));
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // 🔹 Demais excecoes do framework (400, 404, 405, 415...): status original, corpo padronizado
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        String message;
        String error;
        if (status == null) {
            message = "Requisição não pôde ser processada";
            error = "Erro";
        } else {
            switch (status) {
                case BAD_REQUEST -> { message = "Requisição inválida"; error = "Requisição inválida"; }
                case NOT_FOUND -> { message = "Recurso não encontrado"; error = "Recurso não encontrado"; }
                case METHOD_NOT_ALLOWED -> { message = "Método HTTP não suportado para este recurso"; error = "Método não permitido"; }
                case NOT_ACCEPTABLE -> { message = "Formato de resposta não suportado"; error = "Não aceitável"; }
                case UNSUPPORTED_MEDIA_TYPE -> { message = "Tipo de conteúdo não suportado"; error = "Tipo de mídia não suportado"; }
                default -> { message = status.getReasonPhrase(); error = status.getReasonPhrase(); }
            }
        }
        if (statusCode.is5xxServerError()) {
            log.error("Erro do framework ({}) em {}", statusCode.value(), request.getDescription(false), ex);
        } else {
            log.warn("Requisição rejeitada ({}) em {}: {}", statusCode.value(), request.getDescription(false), ex.getMessage());
        }
        ErrorDetails details = new ErrorDetails(LocalDateTime.now(), message, request.getDescription(false),
            statusCode.value(), error);
        return new ResponseEntity<>(details, headers, statusCode);
    }

    // 🔹 Qualquer outra excecao: 500, mas sempre registrada no log
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Erro inesperado em {}", request.getDescription(false), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno no servidor", "Erro interno", request);
    }
}
