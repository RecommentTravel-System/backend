package org.example.wayveesystem.service.impl;


import org.example.wayveesystem.service.AgentMailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Service

public class AgentMailServiceImpl implements AgentMailService {

    @Value("${agentmail.api-key}")
    private String apiKey;

    @Value("${agentmail.inbox-id}")
    private String inboxId;

    private final RestClient restClient = RestClient
            .builder()
            .baseUrl("https://api.agentmail.to")
            .build();

    @Override
    public void sendEmail(
            String to,
            String subject,
            String text,
            String html
    ) {

        Map<String, Object> body = new HashMap<>();

        body.put("to", to);
        body.put("subject", subject);
        body.put("text", text);
        body.put("html", html);

        restClient.post()
                .uri("/v0/inboxes/{inboxId}/messages/send", inboxId)
                .header(
                        "Authorization",
                        "Bearer " + apiKey
                )
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}