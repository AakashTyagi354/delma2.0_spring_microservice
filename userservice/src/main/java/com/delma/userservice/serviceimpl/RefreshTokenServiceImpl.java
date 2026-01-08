package com.delma.userservice.serviceimpl;

import com.delma.userservice.entity.RefreshToken;
import com.delma.userservice.reposistory.RefreshTokenRepository;
import com.delma.userservice.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository repository;

    @Override
    public void save(String token, Long userId, Instant expiry) {
        log.info("Saving refresh token for userId: {}", userId);
        if (userId == null) {
            throw new RuntimeException("userId is NULL while saving refresh token");
        }
        RefreshToken refreshToken = new RefreshToken(null,token,userId,expiry);
        log.info("Refresh token details: {}", refreshToken);
        repository.save(refreshToken);
        log.info("Refresh token saved successfully for userId: {}", userId);
    }

    @Override
    public RefreshToken validate(String token) {
        log.info("Entering inside validate refersh token: {}",token);
        RefreshToken refreshToken = repository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.getExpiry().isBefore(Instant.now())) {
            repository.delete(refreshToken);
            throw new RuntimeException("Refresh token has expired");
        }

        return refreshToken;
    }

    @Override
    public void delete(String token) {
        repository.deleteByToken(token);

    }

    @Override
    public void deleteAllByUser(Long userId) {
            repository.deleteAllByUserId(userId);
    }
}
