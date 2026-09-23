package org.example.wayveesystem.repository;

import org.example.wayveesystem.model.Subscription;
import org.example.wayveesystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.example.wayveesystem.common.enums.SubscriptionPlanType;
import org.example.wayveesystem.common.enums.SubscriptionStatus;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByUser(User user);
    Optional<Subscription> findFirstByUserAndStatusOrderByCreatedAtDesc(User user, SubscriptionStatus status);
    List<Subscription> findByStatus(SubscriptionStatus status);
    List<Subscription> findByPlanType(SubscriptionPlanType planType);
    List<Subscription> findByStatusAndPlanType(SubscriptionStatus status, SubscriptionPlanType planType);
}
