package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public final class BankSimulatorClient {

  private final WebClient webClient;

  public BankSimulatorClient() {
    this.webClient = WebClient.builder()
        .baseUrl("http://localhost:8080") //this wouldn't be hardcoded in production etc
        .build();
  }

  public Mono<BankPaymentResponse> submit(BankPaymentRequest req) {
    return webClient.post()
        .uri("/payments")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(req)
        .retrieve()
        .bodyToMono(BankPaymentResponse.class);
  }

}