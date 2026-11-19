package org.example.wayveesystem.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "website_review_media", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "media_url", length = 1000)
    @Builder.Default
    List<String> mediaUrls = new ArrayList<>();

    @Column(name = "admin_reply", columnDefinition = "TEXT")
    String adminReply;

    @Column(name = "replied_at")
    LocalDateTime repliedAt;

    @Column(name = "replied_by")
    String repliedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}

