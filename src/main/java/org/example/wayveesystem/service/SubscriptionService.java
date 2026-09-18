package org.example.wayveesystem.service;

import org.example.wayveesystem.common.enums.SubscriptionPlanType;
import org.example.wayveesystem.common.enums.SubscriptionStatus;
import org.example.wayveesystem.dto.request.SubscriptionRequest;
import org.example.wayveesystem.dto.request.SubscriptionUpdateRequest;
import org.example.wayveesystem.dto.response.SubscriptionResponse;

import java.util.List;

public interface SubscriptionService {
    SubscriptionResponse createSubscription(SubscriptionRequest request);
    SubscriptionResponse getMyActiveSubscription();
    List<SubscriptionResponse> getMySubscriptions();
    List<SubscriptionResponse> getAllSubscriptions();
    SubscriptionResponse getSubscriptionById(Long subscriptionId);
    SubscriptionResponse updateSubscription(Long subscriptionId, SubscriptionUpdateRequest request);
    SubscriptionResponse cancelSubscription(Long subscriptionId);
    void deleteSubscription(Long subscriptionId);
    List<SubscriptionResponse> searchSubscriptions(SubscriptionStatus status, SubscriptionPlanType planType);
}
