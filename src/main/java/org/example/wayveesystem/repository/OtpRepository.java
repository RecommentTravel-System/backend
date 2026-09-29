package org.example.wayveesystem.repository;

import org.example.wayveesystem.model.OtpEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpRepository extends JpaRepository<OtpEmail, String> {
}
