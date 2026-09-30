package com.agrogestao.common;

import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ForbiddenException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.common.exception.TooManyRequestsException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Nomes dos campos da API como o usuário os vê na tela. */
    private static final Map<String, String> FIELD_LABELS = Map.ofEntries(
            Map.entry("name", "Nome"), Map.entry("email", "E-mail"), Map.entry("password", "Senha"),
            Map.entry("newPassword", "Nova senha"), Map.entry("description", "Descrição"),
            Map.entry("amount", "Valor"), Map.entry("quantity", "Quantidade"),
            Map.entry("unitPrice", "Preço unitário"), Map.entry("unitCost", "Custo unitário"),
            Map.entry("areaHa", "Área"), Map.entry("totalAreaHa", "Área total"),
            Map.entry("productName", "Produto"), Map.entry("unit", "Unidade"),
            Map.entry("minStock", "Estoque mínimo"), Map.entry("notes", "Observações"),
            Map.entry("document", "Documento"), Map.entry("phone", "Telefone"), Map.entry("type", "Tipo"),
            Map.entry("category", "Categoria"), Map.entry("location", "Localização"),
            Map.entry("responsible", "Responsável"), Map.entry("organizationName", "Nome da organização"),
            Map.entry("propertyName", "Nome da propriedade"), Map.entry("items", "Itens"),
            Map.entry("activities", "Atividades"), Map.entry("dueDate", "Vencimento"),
            Map.entry("transactionDate", "Data"), Map.entry("movementDate", "Data"),
            Map.entry("productionDate", "Data"), Map.entry("purchaseDate", "Data"), Map.entry("saleDate", "Data"),
            Map.entry("role", "Papel"), Map.entry("productId", "Produto"), Map.entry("supplierId", "Fornecedor"),
            Map.entry("customerId", "Cliente"), Map.entry("token", "Link"));

    /** "items[0].quantity" -> "Quantidade"; campos sem tradução ficam como estão. */
    private static String label(String fieldPath) {
        String field = fieldPath.substring(fieldPath.lastIndexOf('.') + 1).replaceAll("\\[\\d+]$", "");
        return FIELD_LABELS.getOrDefault(field, field);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiError> handleTooManyRequests(TooManyRequestsException ex) {
        return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Você não tem permissão para acessar este recurso.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> label(error.getField()) + ": " + error.getDefaultMessage())
                .orElse("Dados inválidos");
        return build(HttpStatus.BAD_REQUEST, message);
    }

    /** Corpo ausente, JSON malformado ou valor com tipo errado (ex.: texto onde se espera número ou opção inexistente). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof InvalidFormatException invalid && !invalid.getPath().isEmpty()) {
            String field = invalid.getPath().get(invalid.getPath().size() - 1).getFieldName();
            return build(HttpStatus.BAD_REQUEST, "Valor inválido para o campo " + label(field) + ".");
        }
        return build(HttpStatus.BAD_REQUEST, "Não foi possível ler os dados enviados. Verifique o formato.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro " + ex.getName() + ".");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente: " + ex.getParameterName() + ".");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Recurso não encontrado.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Operação não suportada neste endereço.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Envie os dados em formato JSON.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        return build(HttpStatus.CONFLICT, "Não é possível concluir a operação: existem registros relacionados.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        log.error("Erro interno inesperado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado");
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), status.getReasonPhrase(), message));
    }
}
