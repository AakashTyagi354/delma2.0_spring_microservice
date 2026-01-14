package com.delma.documentservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class AuthUtil {
    @Value("${jwt.secret}")
    private String secretKey;


    private SecretKey getSecretKey() {
        // In a real application, retrieve this key from a secure location
        return Keys.hmacShaKeyFor(secretKey.getBytes((StandardCharsets.UTF_8)));
    }

    public Claims extractClains(String token){
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    public String getUserId(String token) {
        return extractClains(token).get("userId", String.class);
    }

    public String getRole(String token) {
        return extractClains(token).get("role", String.class);
    }
}
