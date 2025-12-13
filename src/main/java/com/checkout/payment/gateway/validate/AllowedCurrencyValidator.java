package com.checkout.payment.gateway.validate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.HashSet;
import java.util.Set;

public class AllowedCurrencyValidator implements ConstraintValidator<AllowedCurrency, String> {

  private Set<String> allowed;

  @Override
  public void initialize(AllowedCurrency ann) {
    this.allowed = new HashSet<>();
    for (String v : ann.value()) {
      allowed.add(v.toUpperCase());
    }
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null || value.isBlank()) return true;
    return allowed.contains(value);
  }
}
