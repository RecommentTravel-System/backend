package org.example.wayveesystem.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "website_reviews")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WebsiteReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user;

    @Column(name = "user_full_name")
    String userFullName;

    @Column(name = "user_email")
    String userEmail;

    @Column(name = "rating", nullable = false)
    Integer rating;

    @Column(name = "category")
    String category;

    @Column(name = "comment", columnDefinition = "TEXT", nullable = false)
    String comment;

    @Column(name = "anonymous")
    @Builder.Default
    Boolean anonymous = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}

