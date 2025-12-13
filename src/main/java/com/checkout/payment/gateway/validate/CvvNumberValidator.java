package com.checkout.payment.gateway.validate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CvvNumberValidator implements ConstraintValidator<Cvv, String> {

  private static final int minLength = 3;
  private static final int maxLength = 4;

  @Override
  public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
    return isDigitsAndLength(s);
  }

  private static boolean isDigitsAndLength(String s) {
    if (s == null) return false;

    int len = s.length();
    if (len < minLength || len > maxLength) return false;

    for (int i = 0; i < len; i++) {
      char c = s.charAt(i);
      if (c < '0' || c > '9') return false;
    }
    return true;
  }

}
