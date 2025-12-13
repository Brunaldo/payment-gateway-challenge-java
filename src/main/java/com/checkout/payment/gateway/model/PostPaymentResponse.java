package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class PostPaymentResponse {

  private UUID id;

  private PaymentStatus status;

  @JsonProperty("card_number_last_four")
  private String cardNumberLastFour;

  @JsonProperty("expiry_month")
  private Integer expiryMonth;

  @JsonProperty("expiry_year")
  private Integer expiryYear;

  private String currency;

  private Long amount;

  @Override
  public String toString() {
    return "GetPaymentResponse{" +
        "id=" + id +
        ", status=" + status +
        ", cardNumberLastFour=" + cardNumberLastFour +
        ", expiryMonth=" + expiryMonth +
        ", expiryYear=" + expiryYear +
        ", currency='" + currency + '\'' +
        ", amount=" + amount +
        '}';
  }
}
