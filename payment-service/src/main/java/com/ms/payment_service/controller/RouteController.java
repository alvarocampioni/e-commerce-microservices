package com.ms.payment_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
public class RouteController {

    @GetMapping("/success")
    public String success() {
        return "Success !";
    }

    @GetMapping("/fail")
    public String fail() {
        return "Fail !";
    }
}
