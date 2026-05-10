package com.delma.userservice.service;

public interface EmailService {
    void sendOtpEmail(String toEmail,String otp);
}
