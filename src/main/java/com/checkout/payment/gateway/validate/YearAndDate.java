package com.checkout.payment.gateway.validate;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = YearAndDateValidator.class)
@Documented
public @interface YearAndDate {
  String message() default "Card has expired";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}