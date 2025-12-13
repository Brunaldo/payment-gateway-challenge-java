package com.checkout.payment.gateway.controller.extension;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValidCreatePaymentRequestBody {
}