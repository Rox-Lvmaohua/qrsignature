package com.qrsignature.service;

import com.qrsignature.controller.dto.SignConfirmRequest;
import com.qrsignature.controller.vo.*;
import com.qrsignature.entity.SignRecord;
import com.qrsignature.entity.UserSignature;
import com.qrsignature.repository.SignRecordRepository;
import com.qrsignature.repository.UserSignatureRepository;
import com.qrsignature.util.JacksonUtils;
import com.qrsignature.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class SignService {

    @Autowired
    private SignRecordRepository signRecordRepository;

    @Autowired
    private UserSignatureRepository userSignatureRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.host:localhost}")
    private String serverHost;

    public SignUrlResponse generateSignUrl(String token, String projectId, String userId, String fileId, String metaCode) {
        // 获取下一个签名序号
        Integer nextSequence = signRecordRepository.getMaxSignatureSequence(projectId, userId, fileId);
        if (nextSequence == null) {
            nextSequence = 1;
        } else {
            nextSequence++;
        }

        // 创建新的签署记录，支持多次签署
        SignRecord signRecord = new SignRecord(projectId, userId, fileId, metaCode);
        signRecord.setSignatureSequence(nextSequence);
        signRecord = signRecordRepository.save(signRecord);

        if (!StringUtils.hasText(token)) {
            token = jwtUtil.generateToken(projectId, userId, fileId, metaCode);
        } else if (!jwtUtil.validateToken(token)) {
            token = jwtUtil.generateToken(projectId, userId, fileId, metaCode);
        }
        String signUrl = String.format("http://%s:%s/sign.html?token=Bearer %s", serverHost, serverPort, token);

        Map<String, Object> redisData = new HashMap<>();
        redisData.put("projectId", projectId);
        redisData.put("userId", userId);
        redisData.put("fileId", fileId);
        redisData.put("metaCode", metaCode);
        redisData.put("signRecordId", signRecord.getId());

        redisTemplate.opsForValue().set(token, JacksonUtils.toJsonString(redisData), Duration.ofMinutes(15));

        SignUrlResponse response = new SignUrlResponse();
        response.setSignUrl(signUrl);
        response.setToken("Bearer " + token);
        response.setStatus(signRecord.getStatus().getDescription());
        response.setSignatureSequence(nextSequence);
        response.setSignatureId(signRecord.getId());

        return response;
    }

    public SignStatusResponse checkSignStatus(String signRecordId) {
        // 获取最新的有效签署记录
        Optional<SignRecord> signRecord = signRecordRepository.findById(signRecordId);

        if (signRecord.isEmpty()) {
            throw new RuntimeException("签署记录不存在");
        }

        SignRecord record = signRecord.get();
        SignStatusResponse response = new SignStatusResponse();
        response.setStatus(record.getStatus().getDescription());
        response.setSignatureSequence(record.getSignatureSequence());

        if (record.getStatus() == SignRecord.SignStatus.SIGNED) {
            response.setSignatureBase64(record.getSignatureBase64());
        }

        return response;
    }

    public SignConfirmResponse confirmSign(String token, SignConfirmRequest request) {
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("无效的token");
        }

        Map<String, Object> redisData = JacksonUtils.readJson((String) redisTemplate.opsForValue().get(token), Map.class);
        if (redisData == null) {
            throw new RuntimeException("token已过期或不存在");
        }

        String signRecordId = redisData.get("signRecordId").toString();
        SignRecord signRecord = signRecordRepository.findById(signRecordId)
                .orElseThrow(() -> new RuntimeException("签署记录不存在"));

        if (signRecord.getStatus() == SignRecord.SignStatus.SIGNED) {
            throw new RuntimeException("该签署请求已完成");
        }

        String signatureBase64 = request.getSignatureBase64();

        // 如果使用历史签名
        if (request.getUserSignatureId() != null && !request.getUserSignatureId().isEmpty()) {
            Optional<UserSignature> userSignature = userSignatureRepository.findById(request.getUserSignatureId());
            if (userSignature.isPresent()) {
                signatureBase64 = userSignature.get().getSignatureBase64();
                signRecord.setUserSignatureId(request.getUserSignatureId());
            }
        } else if (request.getSaveForReuse() != null && request.getSaveForReuse()) {
            // 保存用户签名以便重用
            saveUserSignature(signRecord.getUserId(), signatureBase64, request.getSignatureName(), request.getSetAsDefault());
        }

        signRecord.setStatus(SignRecord.SignStatus.SIGNED);
        signRecord.setSignatureBase64(signatureBase64);
        signRecordRepository.save(signRecord);


        redisTemplate.delete(token);

        SignConfirmResponse response = new SignConfirmResponse();
        response.setMessage("签署成功");
        response.setStatus(SignRecord.SignStatus.SIGNED.getDescription());
        response.setSignatureBase64(signatureBase64);
        response.setSignRecordId(signRecordId);
        response.setSignatureSequence(signRecord.getSignatureSequence());

        return response;
    }

    /**
     * 保存用户签名以便重用
     */
    private void saveUserSignature(String userId, String signatureBase64, String signatureName, Boolean setAsDefault) {
        if (signatureName == null || signatureName.isEmpty()) {
            signatureName = "签名-" + System.currentTimeMillis();
        }

        UserSignature userSignature = new UserSignature(userId, signatureBase64, signatureName);
        userSignature = userSignatureRepository.save(userSignature);

        // 设置为默认签名
        if (setAsDefault != null && setAsDefault) {
            userSignatureRepository.resetDefaultSignatures(userId);
            userSignature.setIsDefault(true);
            userSignatureRepository.save(userSignature);
        }
    }

    /**
     * 获取用户历史签名
     */
    public List<UserSignatureVO> getUserSignatures(String userId) {
        List<UserSignature> signatures = userSignatureRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return signatures.stream()
                .map(this::convertToUserSignatureVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户签名历史记录
     */
    public SignHistoryResponse getUserSignHistory(String userId) {
        List<SignRecord> signRecords = signRecordRepository.findByUserId(userId);
        List<UserSignature> userSignatures = userSignatureRepository.findByUserIdOrderByCreatedAtDesc(userId);

        SignHistoryResponse response = new SignHistoryResponse();

        response.setSignRecords(signRecords.stream()
                .filter(record -> record.getStatus() != SignRecord.SignStatus.DELETED)
                .map(this::convertToSignRecordVO)
                .collect(Collectors.toList()));

        response.setUserSignatures(userSignatures.stream()
                .map(this::convertToUserSignatureVO)
                .collect(Collectors.toList()));

        return response;
    }

    /**
     * 删除签名记录
     */
    public void deleteSignRecord(String signRecordId) {
        signRecordRepository.markAsDeleted(signRecordId);
    }

    /**
     * 删除用户签名
     */
    public void deleteUserSignature(String userId, String signatureId) {
        userSignatureRepository.deleteByUserIdAndId(userId, signatureId);
    }

    /**
     * 获取签名图片
     */
    public String getSignatureImage(String signRecordId) {
        SignRecord signRecord = signRecordRepository.findById(signRecordId)
                .orElseThrow(() -> new RuntimeException("签名记录不存在"));

        if (signRecord.getStatus() != SignRecord.SignStatus.SIGNED) {
            throw new RuntimeException("签名未完成");
        }

        return signRecord.getSignatureBase64();
    }

    private UserSignatureVO convertToUserSignatureVO(UserSignature userSignature) {
        UserSignatureVO vo = new UserSignatureVO();
        vo.setId(userSignature.getId());
        vo.setUserId(userSignature.getUserId());
        vo.setSignatureName(userSignature.getSignatureName());
        vo.setSignatureBase64(userSignature.getSignatureBase64());
        vo.setIsDefault(userSignature.getIsDefault());
        vo.setCreatedAt(userSignature.getCreatedAt());
        vo.setUpdatedAt(userSignature.getUpdatedAt());
        return vo;
    }

    private SignHistoryResponse.SignRecordVO convertToSignRecordVO(SignRecord signRecord) {
        SignHistoryResponse.SignRecordVO vo = new SignHistoryResponse.SignRecordVO();
        vo.setId(signRecord.getId());
        vo.setProjectId(signRecord.getProjectId());
        vo.setUserId(signRecord.getUserId());
        vo.setFileId(signRecord.getFileId());
        vo.setMetaCode(signRecord.getMetaCode());
        vo.setStatus(signRecord.getStatus().getDescription());
        vo.setSignatureBase64(signRecord.getSignatureBase64());
        vo.setSignatureSequence(signRecord.getSignatureSequence());
        vo.setUserSignatureId(signRecord.getUserSignatureId());
        vo.setCreateTime(signRecord.getCreateTime());
        vo.setUpdateTime(signRecord.getUpdateTime());
        return vo;
    }

    public String getBase64Signature(String projectId, String userId, String fileId) {
        Optional<SignRecord> signRecord = signRecordRepository
                .findByProjectIdAndUserIdAndFileId(projectId, userId, fileId);

        if (signRecord.isEmpty()) {
            throw new RuntimeException("签署记录不存在");
        }

        SignRecord record = signRecord.get();
        if (record.getStatus() != SignRecord.SignStatus.SIGNED) {
            throw new RuntimeException("签署记录未完成");
        }
        return record.getSignatureBase64();
    }
}