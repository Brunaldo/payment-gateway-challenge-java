package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.ApiSuccess;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.model.ResponseCode;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("api/v1/")
public class PaymentGatewayController {

  private final PaymentGatewayService paymentGatewayService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
    this.paymentGatewayService = paymentGatewayService;
  }

  @GetMapping(value = "/payment/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ApiSuccess<PostPaymentResponse>> getPostPaymentEventById(@PathVariable UUID id) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiSuccess.ok(ResponseCode.PAYMENT_FOUND, "Payment has been found", paymentGatewayService.getPaymentById(id)));
  }

  @PostMapping(value = "/payment", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ApiSuccess<UUID>> createPayment(@RequestBody @Valid PostPaymentRequest request) {
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiSuccess.created(ResponseCode.PAYMENT_CREATED, "Payment created", paymentGatewayService.processPayment(request)));
  }

}
