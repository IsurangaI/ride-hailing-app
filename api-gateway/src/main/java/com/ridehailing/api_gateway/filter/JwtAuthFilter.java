package com.ridehailing.api_gateway.filter;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.security.Key;

@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<Object> {

    @Value("${jwt.secret}")
    private String secret;

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            // 1. Check the header exists and starts with "Bearer "
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return unauthorized(exchange, "Missing or malformed Authorization header");
            }

            String token = authHeader.substring(7); // Strip "Bearer "

            try {
                // 2. Validate the token and extract the email claim
                Claims claims = Jwts.parser()
                        .verifyWith((SecretKey) getSignKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String userId = claims.getSubject();   // email stored as subject
                String role   = claims.get("role", String.class);

                // 3. Strip the JWT and add trusted headers for downstream services
                //    The backend service reads X-User-Id — no JWT parsing needed there
                ServerWebExchange modifiedExchange = exchange.mutate()
                        .request(r -> r
                                .header("X-User-Id", userId)
                                .header("X-User-Role", role)
                                .headers(h -> h.remove(HttpHeaders.AUTHORIZATION)) // strip JWT
                        )
                        .build();

                return chain.filter(modifiedExchange);

            } catch (ExpiredJwtException e) {
                return unauthorized(exchange, "Token has expired");
            } catch (JwtException e) {
                return unauthorized(exchange, "Invalid token");
            }
        };
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("X-Auth-Error", message);
        return exchange.getResponse().setComplete();
    }

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}