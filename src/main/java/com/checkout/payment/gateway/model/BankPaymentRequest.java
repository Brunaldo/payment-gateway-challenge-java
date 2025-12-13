package com.checkout.payment.gateway.model;

public record BankPaymentRequest(String card_number, String expiry_date, String currency, long amount, String cvv) {}
