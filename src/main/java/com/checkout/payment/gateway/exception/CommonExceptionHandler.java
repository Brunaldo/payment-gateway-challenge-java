package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@ControllerAdvice
public class CommonExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler(EventProcessingException.class)
  public ResponseEntity<ErrorResponse> handleException(EventProcessingException ex) {
    LOG.error("Exception happened", ex);
    return new ResponseEntity<>(new ErrorResponse("Page not found"),
        HttpStatus.NOT_FOUND);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request
  ) {
    Map<String, String> fieldErrors = new LinkedHashMap<>();
    for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
      fieldErrors.putIfAbsent(toSnakeCase(fe.getField()), fe.getDefaultMessage());
    }
    ex.getBindingResult().getGlobalErrors().forEach(ge ->
        fieldErrors.putIfAbsent("card_year_and_month", ge.getDefaultMessage())
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_ERROR",
        "Validation failed",
        Map.of("fields", fieldErrors)
    ));
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request
  ) {
    String path = (request instanceof ServletWebRequest swr) ? swr.getRequest().getRequestURI() : null;

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", Instant.now().toString());
    body.put("status", 400);
    body.put("error", "Bad Request");
    body.put("code", "INVALID_REQUEST_BODY");
    body.put("message", "Failed to read request");
    body.put("requestId", UUID.randomUUID().toString());
    body.put("path", path);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleUnmanaged(Exception ex, HttpServletRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", Instant.now().toString());
    body.put("status", 500);
    body.put("error", "Internal Server Error");
    body.put("code", "INTERNAL_ERROR");
    body.put("message", "Something went wrong");
    body.put("path", request.getRequestURI());
    body.put("requestId", UUID.randomUUID().toString());

    LOG.error("Something went seriously wrong", ex);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }

  private static Map<String, Object> apiError(HttpStatus status, String code, String message, Object details) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", Instant.now().toString());
    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    body.put("code", code);
    body.put("message", message);
    body.put("requestId", UUID.randomUUID().toString());
    if (details != null) body.put("details", details);
    return body;
  }

  private static String toSnakeCase(String val) {
    return StringUtils.join(
        StringUtils.splitByCharacterTypeCamelCase(val),
        '_'
    ).toLowerCase(Locale.ROOT);
  }

}
