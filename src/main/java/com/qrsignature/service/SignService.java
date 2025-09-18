package com.qrsignature.service;

import com.qrsignature.controller.vo.SignConfirmResponse;
import com.qrsignature.controller.vo.SignStatusResponse;
import com.qrsignature.controller.vo.SignUrlResponse;
import com.qrsignature.entity.SignRecord;
import com.qrsignature.repository.SignRecordRepository;
import com.qrsignature.util.JacksonUtils;
import com.qrsignature.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class SignService {

    @Autowired
    private SignRecordRepository signRecordRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.host:localhost}")
    private String serverHost;

    public SignUrlResponse generateSignUrl(String token, String projectId, String userId, String fileId, String metaCode) {
        Optional<SignRecord> existingRecord = signRecordRepository
                .findByProjectIdAndUserIdAndFileId(projectId, userId, fileId);

        SignRecord signRecord;
        if (existingRecord.isPresent()) {
            signRecord = existingRecord.get();
            if (signRecord.getStatus() == SignRecord.SignStatus.SIGNED) {
                throw new RuntimeException("该签署请求已完成，不可重复签署");
            }
        } else {
            signRecord = new SignRecord(projectId, userId, fileId, metaCode);
            signRecord = signRecordRepository.save(signRecord);
        }

        if (!StringUtils.hasText(token) || !jwtUtil.validateToken(token)) {
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
        // 添加 Bearer 前缀
        response.setToken("Bearer " + token);
        response.setStatus(signRecord.getStatus().getDescription());

        return response;
    }

    public SignStatusResponse checkSignStatus(String signRecordId) {
        Optional<SignRecord> signRecord = signRecordRepository
                .findById(signRecordId);

        if (signRecord.isEmpty()) {
            throw new RuntimeException("签署记录不存在");
        }

        SignRecord record = signRecord.get();
        SignStatusResponse response = new SignStatusResponse();
        response.setMetaCode(record.getMetaCode());
        response.setStatus(record.getStatus().getDescription());

        if (record.getStatus() == SignRecord.SignStatus.SIGNED) {
            response.setSignatureBase64(record.getSignatureBase64());
        }

        return response;
    }

    public SignConfirmResponse confirmSign(String token, String signatureBase64) {
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

        signRecord.setStatus(SignRecord.SignStatus.SIGNED);
        signRecord.setSignatureBase64(signatureBase64);
        signRecordRepository.save(signRecord);

        redisTemplate.delete(token);

        SignConfirmResponse response = new SignConfirmResponse();
        response.setMessage("签署成功");
        response.setStatus(SignRecord.SignStatus.SIGNED.getDescription());
        response.setSignatureBase64(signatureBase64);
        response.setSignRecordId(signRecordId);

        return response;
    }
}