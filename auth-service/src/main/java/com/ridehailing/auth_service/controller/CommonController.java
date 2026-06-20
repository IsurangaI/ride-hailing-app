package com.ridehailing.auth_service.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/auth")
public class CommonController {
    @GetMapping("/health")
    public String healthCheck() {
        return "Auth Service is up and running";
    }

}
