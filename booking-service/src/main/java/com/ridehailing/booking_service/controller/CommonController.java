package com.ridehailing.booking_service.controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/v1/common")
public class CommonController {
    @GetMapping("/health")
    public String healthCheck() {
        return "Booking Service is up and running";
    }

}