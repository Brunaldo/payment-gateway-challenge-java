package com.checkout.payment.gateway.controller.extension;

import com.checkout.payment.gateway.model.Currency;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.junit.jupiter.api.extension.*;

import java.lang.reflect.Parameter;
import java.time.LocalDate;

public class PaymentRequestExtension implements ParameterResolver {

  @Override
  public boolean supportsParameter(ParameterContext pc, ExtensionContext ec) {
    Parameter p = pc.getParameter();
    Class<?> type = p.getType();

    boolean wantsValid = p.isAnnotationPresent(ValidCreatePaymentRequestBody.class);

    return wantsValid && type.equals(PostPaymentRequest.class);
  }

  @Override
  public Object resolveParameter(ParameterContext pc, ExtensionContext ec) {
    Parameter p = pc.getParameter();

    if (p.isAnnotationPresent(ValidCreatePaymentRequestBody.class)) {

      String cardNumber = "378282246310005";
      Integer expiryMonth = 1;
      Integer expiryYear = LocalDate.now().plusYears(1).getYear();
      String cvv = "033";
      Long amount = 1000L;
      Currency currency = Currency.GBP;

      return PostPaymentRequest.builder()
          .cardNumber(cardNumber)
          .expiryMonth(expiryMonth)
          .expiryYear(expiryYear)
          .currency(currency.name())
          .amount(amount)
          .cvv(cvv)
          .build();
    }
    return null;
  }

}