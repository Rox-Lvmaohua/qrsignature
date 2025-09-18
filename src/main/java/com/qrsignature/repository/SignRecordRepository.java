package com.qrsignature.repository;

import com.qrsignature.entity.SignRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SignRecordRepository extends JpaRepository<SignRecord, Long> {

    Optional<SignRecord> findByProjectIdAndUserIdAndFileId(String projectId, String userId, String fileId);

    List<SignRecord> findByProjectId(String projectId);

    List<SignRecord> findByUserId(String userId);

    List<SignRecord> findByFileId(String fileId);

    boolean existsByProjectIdAndUserIdAndFileId(String projectId, String userId, String fileId);

    List<SignRecord> findByStatus(SignRecord.SignStatus status);
}