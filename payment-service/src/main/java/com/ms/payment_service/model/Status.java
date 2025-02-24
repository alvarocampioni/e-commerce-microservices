package com.ms.payment_service.model;

import java.io.Serializable;

public enum Status implements Serializable {
    CREATED, SUCCEEDED, FAILED, CANCELED;
}
