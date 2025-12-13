package com.checkout.payment.gateway.validate;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CardNumberValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface CardNumber {

  String message() default "Invalid card number";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}