package com.checkout.payment.gateway.validate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;

public class CardNumberValidator implements ConstraintValidator<CardNumber, String> {

  private static final int minLength = 14;
  private static final int maxLength = 19;

  @Override
  public boolean isValid(String value, ConstraintValidatorContext ctx) {
    boolean isValidDigits = isDigitsAndLength(value);

    if (!isValidDigits) return false;

    String digits = normalise(value);

    if (!digits.chars().allMatch(Character::isDigit)) {
      return false;
    }

    return LuhnCheckDigit.LUHN_CHECK_DIGIT.isValid(digits);
  }

  //clean up whitespace
  private static String normalise(String input) {
    StringBuilder sb = new StringBuilder(input.length());
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (c >= '0' && c <= '9') sb.append(c);
    }
    return sb.toString();
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
