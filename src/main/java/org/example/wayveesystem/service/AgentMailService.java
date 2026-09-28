package org.example.wayveesystem.service;

public interface AgentMailService {
     void sendEmail(
            String to,
            String subject,
            String text,
            String html
    );

}
