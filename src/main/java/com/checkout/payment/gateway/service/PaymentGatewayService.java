package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final BankSimulatorClient simulatorClient;

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    return paymentsRepository.get(id).orElseThrow(() -> new EventProcessingException("Invalid ID"));
  }

  public UUID processPayment(PostPaymentRequest paymentRequest) {
    UUID id = UUID.randomUUID();

    PostPaymentResponse response = PostPaymentResponse.builder()
        .id(id)
        .cardNumberLastFour(paymentRequest.getCardNumberLastFour())
        .expiryMonth(paymentRequest.getExpiryMonth())
        .expiryYear(paymentRequest.getExpiryYear())
        .currency(paymentRequest.getCurrency())
        .amount(paymentRequest.getAmount())
        .build();

    BankPaymentRequest bankPaymentRequest = new BankPaymentRequest(paymentRequest.getCardNumber(),
        paymentRequest.getExpiryDate(),
        paymentRequest.getCurrency(),
        paymentRequest.getAmount(),
        paymentRequest.getCvv());

    response = callAcquiringBank(bankPaymentRequest, response);

    paymentsRepository.add(response);

    return id;
  }

  private PostPaymentResponse callAcquiringBank(BankPaymentRequest bankPaymentRequest,
      PostPaymentResponse response) {
    BankPaymentResponse res;
    try {
      res = simulatorClient.submit(bankPaymentRequest)
          .block(java.time.Duration.ofSeconds(3));

      response = response.toBuilder()
          .status(res.authorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED)
          .build();

    } catch (WebClientResponseException ex) {
      if (ex.getStatusCode().value() == 503) {
        response = response.toBuilder()
            .status(PaymentStatus.REJECTED)
            .build();
      }
    }
    return response;
  }


}
