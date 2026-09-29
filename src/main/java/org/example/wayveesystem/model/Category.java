package org.example.wayveesystem.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "categories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    Long categoryId;

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "code", unique = true, nullable = false)
    String code;

    @Column(name = "osm_key")
    String osmKey;

    @Column(name = "osm_value")
    String osmValue;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(name = "icon_url")
    String iconUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}
