package com.checkout.payment.gateway.validate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class YearAndDateValidator implements ConstraintValidator<YearAndDate, Object> {

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext ctx) {
    if (value == null) return true;

    try {
      var clazz = value.getClass();
      var monthVal = clazz.getMethod("getExpiryMonth").invoke(value);
      var yearVal  = clazz.getMethod("getExpiryYear").invoke(value);

      if (monthVal == null || yearVal == null) {
        return true;
      }

      int month = Integer.parseInt(monthVal.toString());
      int year = Integer.parseInt(yearVal.toString());

      if (month < 1 || month > 12) return false;

      YearMonth expiry = YearMonth.of(year, month);
      YearMonth now = YearMonth.now();

      return expiry.isAfter(now) || expiry.equals(now);
    } catch (DateTimeParseException | ReflectiveOperationException | NumberFormatException e) {
      return true;
    }
  }
}