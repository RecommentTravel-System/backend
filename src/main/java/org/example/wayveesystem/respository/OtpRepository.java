package org.example.wayveesystem.respository;

import org.example.wayveesystem.entity.OtpEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpRepository extends JpaRepository<OtpEmail, String> {
}
