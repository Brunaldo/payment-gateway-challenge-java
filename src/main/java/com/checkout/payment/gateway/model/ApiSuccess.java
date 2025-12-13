package com.checkout.payment.gateway.model;

import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.UUID;

public record ApiSuccess<T>(
    String timestamp,
    int status,
    ResponseCode code,
    String message,
    String requestId,
    T data
) {
  public static <T> ApiSuccess<T> ok(ResponseCode code, String message, T data) {
    return new ApiSuccess<>(
        Instant.now().toString(),
        HttpStatus.OK.value(),
        code,
        message,
        UUID.randomUUID().toString(),
        data
    );
  }

  public static <T> ApiSuccess<T> created(ResponseCode code, String message, T data) {
    return new ApiSuccess<>(
        Instant.now().toString(),
        HttpStatus.CREATED.value(),
        code,
        message,
        UUID.randomUUID().toString(),
        data
    );
  }

}
