package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.validate.AllowedCurrency;
import com.checkout.payment.gateway.validate.CardNumber;
import com.checkout.payment.gateway.validate.Cvv;
import com.checkout.payment.gateway.validate.YearAndDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@YearAndDate
public class PostPaymentRequest implements Serializable {

  @NotBlank
  @CardNumber
  @JsonProperty("card_number")
  private String cardNumber;

  @Min(1)
  @Max(12)
  @JsonProperty("expiry_month")
  private Integer expiryMonth;

  @Positive
  @JsonProperty("expiry_year")
  private Integer expiryYear;

  @NotBlank
  @AllowedCurrency
  private String currency;

  @Positive
  private Long amount;

  @Cvv
  @NotBlank
  private String cvv;

  @JsonProperty("expiry_date")
  public String getExpiryDate() {
    return String.format("%d/%d", expiryMonth, expiryYear);
  }

  @JsonProperty("card_number_last_four")
  private String cardNumberLastFour;

  @Override
  public String toString() {
    return "PostPaymentRequest{" +
        "cardNumber='" + maskCardNumber(cardNumber) + '\'' +
        ", cardNumberLastFour=" + cardNumberLastFour +
        ", expiryMonth=" + expiryMonth +
        ", expiryYear=" + expiryYear +
        ", currency=" + currency +
        ", amount=" + amount +
        ", cvv=" + cvv +
        '}';
  }

  private static String maskCardNumber(String cardNumber) {
    return cardNumber.substring(cardNumber.length()-4);
  }

}
