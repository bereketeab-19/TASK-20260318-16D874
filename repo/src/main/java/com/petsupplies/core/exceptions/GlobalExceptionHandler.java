package com.petsupplies.core.exceptions;

import com.petsupplies.core.api.ErrorResponse;
import com.petsupplies.product.service.BatchImportException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public org.springframework.http.ResponseEntity<ErrorResponse> handleValidation(
      MethodArgumentNotValidException ex,
      HttpServletRequest request
  ) {
    var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
        .sorted(Comparator.comparing(FieldError::getField))
        .map(fe -> new ErrorResponse.FieldError(fe.getField(), Optional.ofNullable(fe.getDefaultMessage()).orElse("Invalid")))
        .toList();

    return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors, null);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public org.springframework.http.ResponseEntity<ErrorResponse> handleAccessDenied(
      AccessDeniedException ex,
      HttpServletRequest request
  ) {
    return build(HttpStatus.FORBIDDEN, "Forbidden", request, List.of(), null);
  }

  @ExceptionHandler(AuthenticationException.class)
  public org.springframework.http.ResponseEntity<ErrorResponse> handleAuthn(
      AuthenticationException ex,
      HttpServletRequest request
  ) {
    return build(HttpStatus.UNAUTHORIZED, "Unauthorized", request, List.of(), null);
  }

  @ExceptionHandler(ErrorResponseException.class)
  public org.springframework.http.ResponseEntity<ErrorResponse> handleErrorResponseException(
      ErrorResponseException ex,
      HttpServletRequest request
  ) {
    HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
    String message = null;
    Object body = ex.getBody();
    if (body instanceof ProblemDetail) {
      message = ((ProblemDetail) body).getDetail();
    }
    if (message == null) {
      message = ex.getMessage();
    }
    return build(status, message, request, List.of(), null);
  }

  @ExceptionHandler(BatchImportException.class)
  public org.springframework.http.ResponseEntity<ErrorResponse> handleBatchImport(
      BatchImportException ex,
      HttpServletRequest request
  ) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of(), null);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public org.springframework.http.ResponseEntity<ErrorResponse> handleDataIntegrity(
      DataIntegrityViolationException ex,
      HttpServletRequest request
  ) {
    return build(
        HttpStatus.BAD_REQUEST,
        "Data constraint violation (check related fields such as role and merchant scope)",
        request,
        List.of(),
        null
    );
  }

  @ExceptionHandler(Exception.class)
  public org.springframework.http.ResponseEntity<ErrorResponse> handleGeneric(
      Exception ex,
      HttpServletRequest request
  ) {
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", request, List.of(), null);
  }

  private org.springframework.http.ResponseEntity<ErrorResponse> build(
      HttpStatus status,
      String message,
      HttpServletRequest request,
      List<ErrorResponse.FieldError> fieldErrors,
      String traceId
  ) {
    String error = status.getReasonPhrase();
    String path = request.getRequestURI();
    ErrorResponse body = new ErrorResponse(
        Instant.now(),
        status.value(),
        error,
        message,
        path,
        fieldErrors,
        traceId
    );
    return org.springframework.http.ResponseEntity.status(status).body(body);
  }
}

