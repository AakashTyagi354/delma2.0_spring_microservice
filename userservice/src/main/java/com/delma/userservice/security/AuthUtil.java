package com.delma.userservice.security;


import com.delma.userservice.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AuthUtil {
    @Value("${jwt.secret}")
    private String secretKey;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        log.info("Generating access token for user: {}", user.getEmail());
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put(
                "roles",
                user.getRoles()
                        .stream()
                        .map(role -> role.name().replace("ROLE_", "")) // SAFETY
                        .toList()
        );

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail()) // email is better than name
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000*60*10))
                .signWith(getSecretKey())
                .compact();
    }

//    public String getUsernameFromToken(String jwtToken) {
//        Claims claims =  Jwts.parser()
//                .verifyWith(getSecretKey())
//                .build()
//                .parseSignedClaims(jwtToken)
//                .getPayload();
//        return claims.getSubject();
//    }
public Claims extractAllClaims(String token) {
    return Jwts.parser()
            .verifyWith(getSecretKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
}

    public String getUsernameFromToken(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String getUserIdFromToken(String token) {
        return extractAllClaims(token).get("userId", String.class);
    }
}
