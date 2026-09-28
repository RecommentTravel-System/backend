package org.example.wayveesystem.service.impl;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.wayveesystem.common.exception.AppException;
import org.example.wayveesystem.common.exception.ErrorCode;
import org.example.wayveesystem.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final SpringTemplateEngine templateEngine;

    @Value("${resend.api-key}")
    private String resendApiKey;

    @Value("${resend.from-email}")
    private String fromEmail;

    private void sendHtmlEmail(
            String toEmail,
            String subject,
            String templateName,
            Context context
    ) {
        try {
            String htmlContent = templateEngine.process(templateName, context);

            Resend resend = new Resend(resendApiKey);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(toEmail)
                    .subject(subject)
                    .html(htmlContent)
                    .build();

            resend.emails().send(params);

            log.info("Email sent successfully to {}", toEmail);

        } catch (ResendException e) {
            log.error("Failed to send email to {}", toEmail, e);
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        } catch (Exception e) {
            log.error("Unexpected error while sending email to {}", toEmail, e);
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    @Override
    public void sendVerifyEmailOtp(String toEmail, String otp) {
        Context context = new Context();
        context.setVariable("otp", otp);

        sendHtmlEmail(
                toEmail,
                "Verify your email - WayveeSystem",
                "verify-email",
                context
        );
    }

    @Override
    public void sendForgotPasswordOtp(String toEmail, String otp) {
        Context context = new Context();
        context.setVariable("otp", otp);

        sendHtmlEmail(
                toEmail,
                "Reset your password - WayveeSystem",
                "forgot-password",
                context
        );
    }
}