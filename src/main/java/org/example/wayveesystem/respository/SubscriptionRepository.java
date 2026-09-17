package org.example.wayveesystem.respository;

import org.example.wayveesystem.model.Subscription;
import org.example.wayveesystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByUser(User user);
    Optional<Subscription> findFirstByUserAndStatusOrderByCreatedAtDesc(User user, org.example.wayveesystem.common.enums.SubscriptionStatus status);
}
