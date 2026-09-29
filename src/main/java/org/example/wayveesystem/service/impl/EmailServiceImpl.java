package org.example.wayveesystem.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.wayveesystem.service.AgentMailService;
import org.example.wayveesystem.service.EmailService;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final AgentMailService agentMailService;
    private final SpringTemplateEngine templateEngine;

    @Override
    public void sendVerifyEmailOtp(
            String toEmail,
            String otp
    ) {

        Context context = new Context();
        context.setVariable("otp", otp);

        String html = templateEngine.process(
                "verify-email",
                context
        );

        String text = """
                Wayvee - Email Verification

                Your verification code is: %s

                This code will expire in 5 minutes.

                If you did not create a Wayvee account,
                please ignore this email.
                """.formatted(otp);

        agentMailService.sendEmail(
                toEmail,
                "Wayvee - Verification OTP",
                text,
                html
        );
    }

    @Override
    public void sendForgotPasswordOtp(
            String toEmail,
            String otp
    ) {

        Context context = new Context();
        context.setVariable("otp", otp);

        String html = templateEngine.process(
                "forgot-password",
                context
        );

        String text = """
                Wayvee - Reset Password

                Your password reset verification code is: %s

                This code will expire in 5 minutes.

                If you did not request a password reset,
                please ignore this email.
                """.formatted(otp);

        agentMailService.sendEmail(
                toEmail,
                "Wayvee - Reset Password OTP",
                text,
                html
        );
    }
}