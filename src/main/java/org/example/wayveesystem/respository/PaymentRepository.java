package org.example.wayveesystem.respository;

import org.example.wayveesystem.model.Payment;
import org.example.wayveesystem.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findBySubscription(Subscription subscription);
    Optional<Payment> findByTransactionCode(String transactionCode);
}
