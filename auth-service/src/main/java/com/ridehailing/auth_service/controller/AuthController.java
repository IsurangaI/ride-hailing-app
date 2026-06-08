package com.ridehailing.auth_service.controller;

import com.ridehailing.auth_service.config.JwtTokenProvider;
import com.ridehailing.auth_service.constants.Role;
import com.ridehailing.auth_service.model.AuthRequest;
import com.ridehailing.auth_service.service.AuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public String authenticateAndGetToken(@RequestBody AuthRequest authRequest) {
        Authentication authentication = authenticationManager
                .authenticate(
                        new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
                );
        if (authentication.isAuthenticated()) {
            return jwtTokenProvider.generateToken(authentication);
        } else {
            throw new RuntimeException("invalid user request !");
        }
    }

    @PostMapping("/register")
    public String registerUser(@RequestBody AuthRequest authRequest) {
        String email  = authRequest.getEmail();
        String password = authRequest.getPassword();
        Role role = authRequest.getRole();

        authService.register(email, password, role);
        return "User registered successfully";
    }

    @GetMapping("/test")
    public String testAuth() {
        return "Your token works";
    }
}
