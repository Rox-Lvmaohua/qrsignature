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
     * 根据用户ID和签名ID查找签名
     */
    Optional<UserSignature> findByUserIdAndId(String userId, String signatureId);

    /**
     * 删除用户指定签名
     */
    void deleteByUserIdAndId(String userId, String signatureId);

    /**
     * 检查用户是否已存在签名
     */
    @Query("SELECT COUNT(us.userId) > 0 FROM UserSignature us WHERE us.userId = :userId")
    boolean existsByUserId(@Param("userId") String userId);
}