package com.checkout.payment.gateway.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.controller.extension.PaymentRequestExtension;
import com.checkout.payment.gateway.controller.extension.ValidCreatePaymentRequestBody;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.ApiSuccess;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.model.ResponseCode;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(PaymentRequestExtension.class)
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  PaymentsRepository paymentsRepository;

  @Autowired
  ObjectMapper mapper;
  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    UUID id = UUID.randomUUID();
    PostPaymentResponse payment = PostPaymentResponse.builder()
        .id(id)
        .amount(10L)
        .currency("USD")
        .status(PaymentStatus.AUTHORIZED)
        .expiryMonth(12)
        .expiryYear(2024)
        .cardNumberLastFour("4321")
        .build();

    paymentsRepository.add(payment);

    MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/payment/" + payment.getId()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.data.card_number_last_four").value(payment.getCardNumberLastFour()))
        .andExpect(jsonPath("$.data.expiry_month").value(payment.getExpiryMonth()))
        .andExpect(jsonPath("$.data.expiry_year").value(payment.getExpiryYear()))
        .andExpect(jsonPath("$.data.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.data.amount").value(payment.getAmount()))
        .andReturn();

    String actualId = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");

    assertThat(actualId).satisfies(v -> {
      assertThat(isUuid(v)).isTrue();
      assertThat(v).isEqualTo(id.toString());
    });
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Page not found"));
  }

  @Test
  void createPayment_invalidCardNumber_containsNonNumeric_400Response(@ValidCreatePaymentRequestBody PostPaymentRequest request) throws Exception {
    request = request.toBuilder()
        .cardNumber("3782822P6310005")
        .build();

    MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(mapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.timestamp").isNotEmpty())
        .andExpect(jsonPath("$.requestId").value(notNullValue()))
        .andExpect(jsonPath("$.details.fields.card_number").value("Invalid card number"))
        .andReturn();

    String requestId = JsonPath.read(result.getResponse().getContentAsString(), "$.requestId");

    assertThat(isUuid(requestId)).isTrue();
  }

  @Test
  void createPayment_invalidCardNumber_lengthLessThan14_400Response(@ValidCreatePaymentRequestBody PostPaymentRequest request) throws Exception {
    short minCardNumberLength = 14;
    String cardNumber = "1234567891234";

    request = request.toBuilder()
        .cardNumber(cardNumber)
        .build();

    assertThat(cardNumber.length()).isLessThan(minCardNumberLength);

    MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(mapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.timestamp").isNotEmpty())
        .andExpect(jsonPath("$.requestId").value(notNullValue()))
        .andExpect(jsonPath("$.details.fields.card_number").value("Invalid card number"))
        .andReturn();

    String requestId = JsonPath.read(result.getResponse().getContentAsString(), "$.requestId");

    assertThat(isUuid(requestId)).isTrue();
  }

  @Test
  void createPayment_invalidCardNumber_lengthMoreThan19_400Response(@ValidCreatePaymentRequestBody PostPaymentRequest request) throws Exception {
    short maxCardNumberLength = 19;
    String cardNumber = "1234567891234567891234";
    request = request.toBuilder()
        .cardNumber(cardNumber)
        .build();

    assertThat(cardNumber.length()).isGreaterThan(maxCardNumberLength);
    String value = mapper.writeValueAsString(request);

    MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(value))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.timestamp").isNotEmpty())
        .andExpect(jsonPath("$.requestId").value(notNullValue()))
        .andExpect(jsonPath("$.details.fields.card_number").value("Invalid card number"))
        .andReturn();

    String requestId = JsonPath.read(result.getResponse().getContentAsString(), "$.requestId");

    assertThat(isUuid(requestId)).isTrue();
  }

  @Test
  void createPayment_expiryMonth_lessThan1_400Response(@ValidCreatePaymentRequestBody PostPaymentRequest request) throws Exception {
    request = request.toBuilder()
        .expiryMonth(-5)
        .build();

    MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(mapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.timestamp").isNotEmpty())
        .andExpect(jsonPath("$.requestId").value(notNullValue()))
        .andExpect(jsonPath("$.details.fields.expiry_month").value("must be greater than or equal to 1"))
        .andReturn();

    String requestId = JsonPath.read(result.getResponse().getContentAsString(), "$.requestId");

    assertThat(isUuid(requestId)).isTrue();
  }

  @Test
  void createPayment_expiryMonth_moreThan12_400Response(@ValidCreatePaymentRequestBody PostPaymentRequest request) throws Exception {
    request = request.toBuilder()
        .expiryMonth(15)
        .build();

    MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(mapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.timestamp").isNotEmpty())
        .andExpect(jsonPath("$.requestId").value(notNullValue()))
        .andExpect(jsonPath("$.details.fields.expiry_month").value("must be less than or equal to 12"))
        .andReturn();

    String requestId = JsonPath.read(result.getResponse().getContentAsString(), "$.requestId");

    assertThat(isUuid(requestId)).isTrue();
  }

  @Test
  void createPayment_cvv_length2_400Response(@ValidCreatePaymentRequestBody PostPaymentRequest request) throws Exception {
    request = request.toBuilder()
        .cvv("12")
        .build();

    MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(mapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.timestamp").isNotEmpty())
        .andExpect(jsonPath("$.requestId").value(notNullValue()))
        .andExpect(jsonPath("$.details.fields.cvv").value("Invalid Cvv number"))
        .andReturn();

    String requestId = JsonPath.read(result.getResponse().getContentAsString(), "$.requestId");

    assertThat(isUuid(requestId)).isTrue();
  }

  @Test
  void createPayment_cvv_length6_400Response(@ValidCreatePaymentRequestBody PostPaymentRequest request) throws Exception {
    request = request.toBuilder()
        .cvv("123456")
        .build();

    MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(mapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.timestamp").isNotEmpty())
        .andExpect(jsonPath("$.requestId").value(notNullValue()))
        .andExpect(jsonPath("$.details.fields.cvv").value("Invalid Cvv number"))
        .andReturn();

    String requestId = JsonPath.read(result.getResponse().getContentAsString(), "$.requestId");

    assertThat(isUuid(requestId)).isTrue();
  }

  @Test
  void createPayment_cvv_withNonNumeric_400Response(@ValidCreatePaymentRequestBody PostPaymentRequest request) throws Exception {
    request = request.toBuilder()
        .cvv("123P")
        .build();

    MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(mapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.timestamp").isNotEmpty())
        .andExpect(jsonPath("$.requestId").value(notNullValue()))
        .andExpect(jsonPath("$.details.fields.cvv").value("Invalid Cvv number"))
        .andReturn();

    String requestId = JsonPath.read(result.getResponse().getContentAsString(), "$.requestId");

    assertThat(isUuid(requestId)).isTrue();
  }


  @Test
  void createPayment_expiryYear_isInthePast_400Response(@ValidCreatePaymentRequestBody PostPaymentRequest request) throws Exception {
    request = request.toBuilder()
        .expiryYear(LocalDate.now().minusYears(2).getYear())
        .build();

    MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(mapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.timestamp").isNotEmpty())
        .andExpect(jsonPath("$.requestId").value(notNullValue()))
        .andExpect(jsonPath("$.details.fields.card_year_and_month").value("Card has expired"))
        .andReturn();

    String requestId = JsonPath.read(result.getResponse().getContentAsString(), "$.requestId");

    assertThat(isUuid(requestId)).isTrue();
  }

  @Test
  void createPayment_unsupportedCurrency_400Response(@ValidCreatePaymentRequestBody PostPaymentRequest request) throws Exception {
    request = request.toBuilder()
        .currency("DPDPDPDP")
        .build();

    MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(mapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.timestamp").isNotEmpty())
        .andExpect(jsonPath("$.requestId").value(notNullValue()))
        .andExpect(jsonPath("$.details.fields.currency").value("Unsupported currency"))
        .andReturn();

    String requestId = JsonPath.read(result.getResponse().getContentAsString(), "$.requestId");

    assertThat(isUuid(requestId)).isTrue();
  }


  @Test
  void createPayment_amount_isMinus_400Response(@ValidCreatePaymentRequestBody PostPaymentRequest request) throws Exception {
    request = request.toBuilder()
        .amount(-1L)
        .build();

    MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(mapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.timestamp").isNotEmpty())
        .andExpect(jsonPath("$.requestId").value(notNullValue()))
        .andExpect(jsonPath("$.details.fields.amount").value("must be greater than 0"))
        .andReturn();

    String requestId = JsonPath.read(result.getResponse().getContentAsString(), "$.requestId");

    assertThat(isUuid(requestId)).isTrue();
  }

  @Test
  void createPayment_multipleFieldBadFormat_400Response(@ValidCreatePaymentRequestBody PostPaymentRequest request) throws Exception {
    request = request.toBuilder()
        .cardNumber("abc")
        .cvv("o21")
        .currency("unknown currency")
        .expiryMonth(43)
        .expiryYear(120)
        .build();

    MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(mapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.timestamp").isNotEmpty())
        .andExpect(jsonPath("$.requestId").value(notNullValue()))
        .andExpect(jsonPath("$.details.fields.currency").value("Unsupported currency"))
        .andExpect(jsonPath("$.details.fields.card_number").value("Invalid card number"))
        .andExpect(jsonPath("$.details.fields.cvv").value("Invalid Cvv number"))
        .andExpect(jsonPath("$.details.fields.expiry_month").value("must be less than or equal to 12"))
        .andExpect(jsonPath("$.details.fields.card_year_and_month").value("Card has expired"))
        .andReturn();

    String requestId = JsonPath.read(result.getResponse().getContentAsString(), "$.requestId");

    assertThat(isUuid(requestId)).isTrue();
  }

  @Test
  void createPayment_success_201Response(@ValidCreatePaymentRequestBody PostPaymentRequest request) throws Exception {
    MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(mapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andReturn();

    String paymentId = JsonPath.read(result.getResponse().getContentAsString(), "$.data");

    assertThat(isUuid(paymentId)).isTrue();

    assertThat(paymentsRepository.get(UUID.fromString(paymentId)))
        .isPresent();
  }

  @Test
  void acquiringBank_authorisePaymentBankReturnsAuthorisedTrue() throws Exception {
    PostPaymentRequest paymentRequest = PostPaymentRequest.builder()
        .cardNumber("38520000023237")
        .expiryYear(LocalDate.now().plusYears(1).getYear())
        .expiryMonth(6)
        .cvv("344")
        .currency("USD")
        .amount(1500L)
        .build();

    var result = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(paymentRequest)))
        .andExpect(status().isCreated())
        .andReturn();

    ApiSuccess<String> actualSuccessResponse = objectMapper.readValue(result.getResponse().getContentAsString(), ApiSuccess.class);

    assertThat(actualSuccessResponse)
        .satisfies(val -> {
          assertThat(val.timestamp()).isNotBlank();
          assertThat(val.status()).isEqualTo(HttpStatus.CREATED.value());
          assertThat(val.code()).isEqualTo(ResponseCode.PAYMENT_CREATED);
          assertThat(val.message()).isNotBlank();
          assertThat(val.requestId()).isNotBlank();
          assertThat(val.data()).isNotNull();
        });

    Optional<PostPaymentResponse> saved = paymentsRepository.get(UUID.fromString(actualSuccessResponse.data()));
    assertThat(saved).isPresent();
    assertThat(saved.get().getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
  }

  @Test
  void acquiringBank_declinedPaymentBankReturnsAuthorisedFalse() throws Exception {
    PostPaymentRequest paymentRequest = PostPaymentRequest.builder()
        .cardNumber("6011000990139424")
        .expiryYear(LocalDate.now().plusYears(1).getYear())
        .expiryMonth(6)
        .cvv("344")
        .currency("USD")
        .amount(1500L)
        .build();

    var result = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(paymentRequest)))
        .andExpect(status().isCreated())
        .andReturn();

    ApiSuccess<String> actualSuccessResponse = objectMapper.readValue(result.getResponse().getContentAsString(), ApiSuccess.class);

    assertThat(actualSuccessResponse)
        .satisfies(val -> {
          assertThat(val.timestamp()).isNotBlank();
          assertThat(val.status()).isEqualTo(HttpStatus.CREATED.value());
          assertThat(val.code()).isEqualTo(ResponseCode.PAYMENT_CREATED);
          assertThat(val.message()).isNotBlank();
          assertThat(val.requestId()).isNotBlank();
          assertThat(val.data()).isNotNull();
        });

    Optional<PostPaymentResponse> saved = paymentsRepository.get(UUID.fromString(actualSuccessResponse.data()));
    assertThat(saved).isPresent();
    assertThat(saved.get().getStatus()).isEqualTo(PaymentStatus.DECLINED);
  }

  @Test
  void acquiringBank_returnServiceUnavailableWhenBankReturns503() throws Exception {
    PostPaymentRequest paymentRequest = PostPaymentRequest.builder()
        .cardNumber("378734493671000")
        .expiryYear(LocalDate.now().plusYears(1).getYear())
        .expiryMonth(6)
        .cvv("344")
        .currency("GBP")
        .amount(1500L)
        .build();

    var result = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(paymentRequest)))
        .andExpect(status().isCreated())
        .andReturn();

    ApiSuccess<String> actualSuccessResponse = objectMapper.readValue(result.getResponse().getContentAsString(), ApiSuccess.class);

    assertThat(actualSuccessResponse)
        .satisfies(val -> {
          assertThat(val.timestamp()).isNotBlank();
          assertThat(val.status()).isEqualTo(HttpStatus.CREATED.value());
          assertThat(val.code()).isEqualTo(ResponseCode.PAYMENT_CREATED);
          assertThat(val.message()).isNotBlank();
          assertThat(val.requestId()).isNotBlank();
          assertThat(val.data()).isNotNull();
        });

    Optional<PostPaymentResponse> saved = paymentsRepository.get(UUID.fromString(actualSuccessResponse.data()));
    assertThat(saved).isPresent();
    assertThat(saved.get().getStatus()).isEqualTo(PaymentStatus.REJECTED);
  }

  @Test
  void getRecord_nonExistent_returns404() throws Exception {
    String randomUuid = UUID.randomUUID().toString();

    mvc.perform(MockMvcRequestBuilders.get("/payment/" + randomUuid))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  private static boolean isUuid(String val) {
    try {
      UUID.fromString(val);
      return true;
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

}
