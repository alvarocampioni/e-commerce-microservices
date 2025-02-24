package com.ms.user_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum Role implements Serializable {
    CUSTOMER("CUSTOMER"),
    ADMIN("ADMIN");

    private String name;
}
