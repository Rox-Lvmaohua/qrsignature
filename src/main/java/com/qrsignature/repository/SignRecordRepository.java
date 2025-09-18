package com.qrsignature.repository;

import com.qrsignature.entity.SignRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SignRecordRepository extends JpaRepository<SignRecord, String> {

    Optional<SignRecord> findByProjectIdAndUserIdAndFileId(String projectId, String userId, String fileId);

    /**
     * 查找最新的有效签署记录
     */
    Optional<SignRecord> findFirstByProjectIdAndUserIdAndFileIdAndStatusNotOrderBySignatureSequenceDescCreateTimeDesc(
            String projectId, String userId, String fileId, SignRecord.SignStatus status);


    /**
     * 查找指定项目用户文件的所有签署记录（包括已删除的）
     */
    List<SignRecord> findByProjectIdAndUserIdAndFileIdOrderBySignatureSequenceDesc(String projectId, String userId, String fileId);

    /**
     * 查找所有有效的签署记录（排除已删除的）
     */
    List<SignRecord> findByProjectIdAndUserIdAndFileIdAndStatusNotOrderBySignatureSequenceDesc(
            String projectId, String userId, String fileId, SignRecord.SignStatus status);

    List<SignRecord> findByProjectId(String projectId);

    List<SignRecord> findByUserId(String userId);

    List<SignRecord> findByFileId(String fileId);

    /**
     * 获取下一个签名序号
     */
    @Query("SELECT MAX(s.signatureSequence) FROM SignRecord s WHERE s.projectId = :projectId AND s.userId = :userId AND s.fileId = :fileId")
    Integer getMaxSignatureSequence(@Param("projectId") String projectId, @Param("userId") String userId, @Param("fileId") String fileId);

    List<SignRecord> findByStatus(SignRecord.SignStatus status);

    /**
     * 标记为已删除
     */
    @Modifying
    @Query("UPDATE SignRecord s SET s.status = 'DELETED' WHERE s.id = :signRecordId")
    void markAsDeleted(@Param("signRecordId") String signRecordId);

    /**
     * 删除记录
     */
    void deleteByProjectIdAndUserIdAndFileId(String projectId, String userId, String fileId);
}