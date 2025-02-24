package com.ms.payment_service.model;

public enum StripeEventTypes {
    CHECKOUT_SESSION_COMPLETED("checkout.session.completed"),
    PAYMENT_INTENT_SUCCEEDED("payment_intent.succeeded"),
    PAYMENT_INTENT_FAILED("payment_intent.payment_failed"),
    CHECKOUT_SESSION_EXPIRED("checkout.session.expired");

    public final String name;

    StripeEventTypes(String name) {
        this.name = name;
    }

    public static StripeEventTypes fromString(String name) {
        for (StripeEventTypes type : StripeEventTypes.values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return null;
    }
}
