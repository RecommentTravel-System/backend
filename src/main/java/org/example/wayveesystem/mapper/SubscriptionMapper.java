package org.example.wayveesystem.mapper;

import org.example.wayveesystem.common.enums.SubscriptionStatus;
import org.example.wayveesystem.dto.request.SubscriptionRequest;
import org.example.wayveesystem.dto.request.SubscriptionUpdateRequest;
import org.example.wayveesystem.dto.response.SubscriptionResponse;
import org.example.wayveesystem.model.Subscription;
import org.example.wayveesystem.model.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SubscriptionMapper {

    public Subscription toSubscription(SubscriptionRequest request, User user) {
        if (request == null) {
            return null;
        }
        LocalDateTime start = request.getStartDate() != null ? request.getStartDate() : LocalDateTime.now();
        SubscriptionStatus status = request.getStatus() != null ? request.getStatus() : SubscriptionStatus.ACTIVE;

        return Subscription.builder()
                .user(user)
                .planName(request.getPlanName())
                .planType(request.getPlanType())
                .price(request.getPrice())
                .startDate(start)
                .endDate(request.getEndDate())
                .status(status)
                .build();
    }

    public SubscriptionResponse toSubscriptionResponse(Subscription subscription) {
        if (subscription == null) {
            return null;
        }
        return SubscriptionResponse.builder()
                .subscriptionId(subscription.getSubscriptionId())
                .userId(subscription.getUser() != null ? subscription.getUser().getUserId() : null)
                .userEmail(subscription.getUser() != null ? subscription.getUser().getEmail() : null)
                .userFullName(subscription.getUser() != null ? subscription.getUser().getFullName() : null)
                .planName(subscription.getPlanName())
                .planType(subscription.getPlanType())
                .price(subscription.getPrice())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .status(subscription.getStatus())
                .createdAt(subscription.getCreatedAt())
                .build();
    }

    public void updateSubscription(Subscription subscription, SubscriptionUpdateRequest request) {
        if (subscription == null || request == null) {
            return;
        }
        if (request.getPlanName() != null) subscription.setPlanName(request.getPlanName());
        if (request.getPlanType() != null) subscription.setPlanType(request.getPlanType());
        if (request.getPrice() != null) subscription.setPrice(request.getPrice());
        if (request.getStartDate() != null) subscription.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) subscription.setEndDate(request.getEndDate());
        if (request.getStatus() != null) subscription.setStatus(request.getStatus());
    }
}
