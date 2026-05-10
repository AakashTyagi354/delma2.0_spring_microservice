package com.delma.userservice.serviceimpl;

import com.delma.userservice.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;


@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final long OTP_EXPIRY_MINUTES = 10;
    private static final String OTP_PREFIX = "otp:";


    @Override
    public String generateAndStoreOtp(String email) {
        String otp = String.valueOf((int)(Math.random()*900000)+100000);

        String key = OTP_PREFIX + email;
        redisTemplate.opsForValue().set(key,otp,OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);

        log.info("OTP generated and stored for email: {}", email);
        return otp;
    }

    @Override
    public boolean verifyOtp(String email, String otp) {
        String key = OTP_PREFIX + email;
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null) {
            log.warn("OTP expired or not found for email: {}", email);
            return false;
        }

        if (storedOtp.equals(otp)) {
            // Delete OTP after successful verification — one-time use
            redisTemplate.delete(key);
            log.info("OTP verified successfully for email: {}", email);
            return true;
        }

        log.warn("Invalid OTP attempt for email: {}", email);
        return false;
    }
}
