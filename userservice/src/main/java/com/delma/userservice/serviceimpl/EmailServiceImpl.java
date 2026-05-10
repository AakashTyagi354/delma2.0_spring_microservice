package com.delma.userservice.serviceimpl;

import com.delma.common.exception.BadRequestException;
import com.delma.userservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;


    @Override
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Delma Health — Email Verification OTP");
            message.setText(
                    "Hello,\n\n" +
                            "Your OTP for Delma Health email verification is:\n\n" +
                            "🔐 " + otp + "\n\n" +
                            "This OTP is valid for 10 minutes.\n" +
                            "Do not share this OTP with anyone.\n\n" +
                            "If you did not request this, please ignore this email.\n\n" +
                            "— Delma Health Team"
            );
            mailSender.send(message);
            log.info("OTP email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {} — Cause: {}",
                    toEmail, e.getMessage(),
                    e.getCause() != null ? e.getCause().getMessage() : "no cause");
            throw new BadRequestException("Failed to send verification email. Please try again.");
        }
    }
}
