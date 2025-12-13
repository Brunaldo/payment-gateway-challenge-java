package com.checkout.payment.gateway.validate;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AllowedCurrencyValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedCurrency {
  String message() default "Unsupported currency";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};

  String[] value() default {"EUR", "GBP", "USD"};
}