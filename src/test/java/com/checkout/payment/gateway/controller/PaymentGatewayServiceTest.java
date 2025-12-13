package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.checkout.payment.gateway.service.BankSimulatorClient;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

  @Mock PaymentsRepository paymentsRepository;
  @Mock BankSimulatorClient simulatorClient;

  @InjectMocks PaymentGatewayService service;

  @Captor ArgumentCaptor<PostPaymentResponse> responseCaptor;
  @Captor ArgumentCaptor<BankPaymentRequest> bankReqCaptor;

  @Test
  void getPaymentById_returnsPaymentWhenFound() {
    UUID id = UUID.randomUUID();
    PostPaymentResponse expected = PostPaymentResponse.builder().id(id).build();

    when(paymentsRepository.get(id)).thenReturn(Optional.of(expected));

    PostPaymentResponse actual = service.getPaymentById(id);

    assertThat(actual).isSameAs(expected);
    verify(paymentsRepository).get(id);
    verifyNoMoreInteractions(paymentsRepository, simulatorClient);
  }

  @Test
  void getPaymentById_throwsWhenNotFound() {
    UUID id = UUID.randomUUID();
    when(paymentsRepository.get(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getPaymentById(id))
        .isInstanceOf(EventProcessingException.class)
        .hasMessageContaining("Invalid ID");

    verify(paymentsRepository).get(id);
    verifyNoMoreInteractions(paymentsRepository, simulatorClient);
  }

  @Test
  void processPayment_authorizesWhenBankReturnsAuthorizedTrue() {
    PostPaymentRequest req = mockValidRequest();
    when(simulatorClient.submit(any(BankPaymentRequest.class)))
        .thenReturn(Mono.just(new BankPaymentResponse(true, UUID.randomUUID().toString())));

    UUID id = service.processPayment(req);

    assertThat(id).isNotNull();

    verify(simulatorClient).submit(bankReqCaptor.capture());
    BankPaymentRequest bankReq = bankReqCaptor.getValue();
    assertThat(bankReq).isNotNull();

    verify(paymentsRepository).add(responseCaptor.capture());
    PostPaymentResponse saved = responseCaptor.getValue();

    assertThat(saved.getId()).isEqualTo(id);
    assertThat(saved.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
  }

  @Test
  void processPayment_declinesWhenBankReturnsAuthorizedFalse() {
    PostPaymentRequest req = mockValidRequest();
    when(simulatorClient.submit(any(BankPaymentRequest.class)))
        .thenReturn(Mono.just(new BankPaymentResponse(false, null)));

    UUID id = service.processPayment(req);

    verify(paymentsRepository).add(responseCaptor.capture());
    PostPaymentResponse saved = responseCaptor.getValue();

    assertThat(saved.getId()).isEqualTo(id);
    assertThat(saved.getStatus()).isEqualTo(PaymentStatus.DECLINED);
  }

  @Test
  void processPayment_rejectsWhenBankReturns503() {
    PostPaymentRequest req = mockValidRequest();

    WebClientResponseException ex = WebClientResponseException.create(
        503,
        "Service Unavailable",
        null,
        null,
        StandardCharsets.UTF_8
    );

    when(simulatorClient.submit(any(BankPaymentRequest.class)))
        .thenReturn(Mono.error(ex));

    UUID id = service.processPayment(req);

    verify(paymentsRepository).add(responseCaptor.capture());
    PostPaymentResponse saved = responseCaptor.getValue();

    assertThat(saved.getId()).isEqualTo(id);
    assertThat(saved.getStatus()).isEqualTo(PaymentStatus.REJECTED);
  }

  //https://www.paypalobjects.com/en_AU/vhelp/paypalmanager_help/credit_card_numbers.htm
  private PostPaymentRequest mockValidRequest() {
    PostPaymentRequest req = mock(PostPaymentRequest.class);

    when(req.getCardNumberLastFour()).thenReturn("1117");
    when(req.getExpiryMonth()).thenReturn(12);
    when(req.getExpiryYear()).thenReturn(2025);
    when(req.getCurrency()).thenReturn("GBP");
    when(req.getAmount()).thenReturn(1500L);

    when(req.getCardNumber()).thenReturn("6011111111111117");
    when(req.getExpiryDate()).thenReturn("12/2025");
    when(req.getCvv()).thenReturn("334");

    return req;
  }
}