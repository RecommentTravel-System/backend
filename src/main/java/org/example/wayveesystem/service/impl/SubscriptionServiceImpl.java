package org.example.wayveesystem.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.enums.Role;
import org.example.wayveesystem.common.enums.SubscriptionPlanType;
import org.example.wayveesystem.common.enums.SubscriptionStatus;
import org.example.wayveesystem.common.exception.AppException;
import org.example.wayveesystem.common.exception.ErrorCode;
import org.example.wayveesystem.dto.request.SubscriptionRequest;
import org.example.wayveesystem.dto.request.SubscriptionUpdateRequest;
import org.example.wayveesystem.dto.response.SubscriptionResponse;
import org.example.wayveesystem.mapper.SubscriptionMapper;
import org.example.wayveesystem.model.Subscription;
import org.example.wayveesystem.model.User;
import org.example.wayveesystem.respository.SubscriptionRepository;
import org.example.wayveesystem.respository.UserRepository;
import org.example.wayveesystem.service.SubscriptionService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionServiceImpl implements SubscriptionService {

    SubscriptionRepository subscriptionRepository;
    UserRepository userRepository;
    SubscriptionMapper subscriptionMapper;

    private User getCurrentUser() {
        String userIdStr = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(Long.valueOf(userIdStr))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private void checkPermission(User currentUser, Subscription subscription) {
        if (!subscription.getUser().getUserId().equals(currentUser.getUserId()) && currentUser.getRole() != Role.ADMIN) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }
    }

    @Override
    public SubscriptionResponse createSubscription(SubscriptionRequest request) {
        User currentUser = getCurrentUser();
        Subscription subscription = subscriptionMapper.toSubscription(request, currentUser);
        subscription = subscriptionRepository.save(subscription);
        return subscriptionMapper.toSubscriptionResponse(subscription);
    }

    @Override
    public SubscriptionResponse getMyActiveSubscription() {
        User currentUser = getCurrentUser();
        Subscription subscription = subscriptionRepository
                .findFirstByUserAndStatusOrderByCreatedAtDesc(currentUser, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
        return subscriptionMapper.toSubscriptionResponse(subscription);
    }

    @Override
    public List<SubscriptionResponse> getMySubscriptions() {
        User currentUser = getCurrentUser();
        return subscriptionRepository.findByUser(currentUser).stream()
                .map(subscriptionMapper::toSubscriptionResponse)
                .toList();
    }

    @Override
    public List<SubscriptionResponse> getAllSubscriptions() {
        return subscriptionRepository.findAll().stream()
                .map(subscriptionMapper::toSubscriptionResponse)
                .toList();
    }

    @Override
    public SubscriptionResponse getSubscriptionById(Long subscriptionId) {
        User currentUser = getCurrentUser();
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        checkPermission(currentUser, subscription);

        return subscriptionMapper.toSubscriptionResponse(subscription);
    }

    @Override
    public SubscriptionResponse updateSubscription(Long subscriptionId, SubscriptionUpdateRequest request) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        subscriptionMapper.updateSubscription(subscription, request);
        subscription = subscriptionRepository.save(subscription);
        return subscriptionMapper.toSubscriptionResponse(subscription);
    }

    @Override
    public SubscriptionResponse cancelSubscription(Long subscriptionId) {
        User currentUser = getCurrentUser();
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        checkPermission(currentUser, subscription);

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription = subscriptionRepository.save(subscription);
        return subscriptionMapper.toSubscriptionResponse(subscription);
    }

    @Override
    public void deleteSubscription(Long subscriptionId) {
        if (!subscriptionRepository.existsById(subscriptionId)) {
            throw new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND);
        }
        subscriptionRepository.deleteById(subscriptionId);
    }

    @Override
    public List<SubscriptionResponse> searchSubscriptions(SubscriptionStatus status, SubscriptionPlanType planType) {
        List<Subscription> subscriptions;
        if (status != null && planType != null) {
            subscriptions = subscriptionRepository.findByStatusAndPlanType(status, planType);
        } else if (status != null) {
            subscriptions = subscriptionRepository.findByStatus(status);
        } else if (planType != null) {
            subscriptions = subscriptionRepository.findByPlanType(planType);
        } else {
            subscriptions = subscriptionRepository.findAll();
        }

        return subscriptions.stream()
                .map(subscriptionMapper::toSubscriptionResponse)
                .toList();
    }
}
