package com.qrsignature.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author Administrator
 */
@Entity
@Table(name = "sign_record")
@EntityListeners(AuditingEntityListener.class)
@Data
public class SignRecord {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "project_id")
    private String projectId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "file_id")
    private String fileId;

    @Column(name = "meta_code")
    private String metaCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SignStatus status = SignStatus.UNSCANNED;

    @Column(length = 1000000)
    private String signatureBase64;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updateTime;

    @Getter
    public enum SignStatus {
        UNSCANNED("未扫描"),
        SCANNED_UNCONFIRMED("已扫描未签署"),
        SIGNED("已签署");

        private final String description;

        SignStatus(String description) {
            this.description = description;
        }

    }


    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    public SignRecord() {}

    public SignRecord(String projectId, String userId, String fileId, String metaCode) {
        this.projectId = projectId;
        this.userId = userId;
        this.fileId = fileId;
        this.metaCode = metaCode;
    }
}