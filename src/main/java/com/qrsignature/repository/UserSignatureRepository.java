package com.qrsignature.repository;

import com.qrsignature.entity.UserSignature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSignatureRepository extends JpaRepository<UserSignature, String> {

    /**
     * 根据用户ID查找所有签名
     */
    List<UserSignature> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * 根据用户ID查找默认签名
     */
    Optional<UserSignature> findByUserIdAndIsDefaultTrue(String userId);

    /**
     * 根据用户ID和签名ID查找签名
     */
    Optional<UserSignature> findByUserIdAndId(String userId, String signatureId);

    /**
     * 将用户的所有签名设为非默认
     */
    @Modifying
    @Query("UPDATE UserSignature u SET u.isDefault = false WHERE u.userId = :userId")
    void resetDefaultSignatures(@Param("userId") String userId);

    /**
     * 删除用户指定签名
     */
    void deleteByUserIdAndId(String userId, String signatureId);
}