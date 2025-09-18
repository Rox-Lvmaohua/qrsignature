package com.qrsignature.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * @author Administrator
 */
@Entity
@Table(name = "user_signatures",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id"}, name = "user_id_unique_idx"),
        })
@Data
@NoArgsConstructor
public class UserSignature {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "signature_base64", nullable = false, columnDefinition = "TEXT")
    private String signatureBase64;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UserSignature(String userId, String signatureBase64) {
        this.userId = userId;
        this.signatureBase64 = signatureBase64;
    }
}