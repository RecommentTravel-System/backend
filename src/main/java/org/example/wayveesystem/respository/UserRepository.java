package org.example.wayveesystem.respository;

import org.example.wayveesystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmail(String username, String email);
    boolean existsByEmail(String email);
}
