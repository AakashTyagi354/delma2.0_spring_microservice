package com.delma.userservice.service;

import com.delma.userservice.entity.RefreshToken;

import java.time.Instant;

public interface RefreshTokenService {
    public void save(String token, Long userId, Instant expiry);
    public RefreshToken validate(String token);
    public void delete(String token);
    public void deleteAllByUser(Long userId);


}
