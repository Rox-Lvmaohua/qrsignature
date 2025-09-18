package com.qrsignature.repository;

import com.qrsignature.entity.SignRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SignRecordRepository extends JpaRepository<SignRecord, String> {

    Optional<SignRecord> findByProjectIdAndUserIdAndFileId(String projectId, String userId, String fileId);

    List<SignRecord> findByProjectId(String projectId);

    List<SignRecord> findByUserId(String userId);

    List<SignRecord> findByFileId(String fileId);

    boolean existsByProjectIdAndUserIdAndFileId(String projectId, String userId, String fileId);

    List<SignRecord> findByStatus(SignRecord.SignStatus status);

    @Query("SELECT MAX(s.signatureSequence) FROM SignRecord s WHERE s.projectId = ?1 AND s.userId = ?2 AND s.fileId = ?3")
    Integer getMaxSignatureSequence(String projectId, String userId, String fileId);
}