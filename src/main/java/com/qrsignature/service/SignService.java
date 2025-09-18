package com.qrsignature.service;

import com.qrsignature.entity.SignRecord;
import com.qrsignature.repository.SignRecordRepository;
import com.qrsignature.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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

    public Map<String, Object> generateSignUrl(String projectId, String userId, String fileId, String metaCode) {
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

        String token = jwtUtil.generateToken(projectId, userId, fileId, metaCode);
        String signUrl = String.format("http://%s:%s/api/sign/%s", serverHost, serverPort, token);

        Map<String, Object> redisData = new HashMap<>();
        redisData.put("projectId", projectId);
        redisData.put("userId", userId);
        redisData.put("fileId", fileId);
        redisData.put("metaCode", metaCode);
        redisData.put("signRecordId", signRecord.getId());

        redisTemplate.opsForValue().set(token, redisData, Duration.ofMinutes(15));

        Map<String, Object> result = new HashMap<>();
        result.put("signUrl", signUrl);
        result.put("token", token);
        result.put("status", signRecord.getStatus().getDescription());

        return result;
    }

    public Map<String, Object> validateToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("无效的token");
        }

        Map<String, Object> redisData = (Map<String, Object>) redisTemplate.opsForValue().get(token);
        if (redisData == null) {
            throw new RuntimeException("token已过期或不存在");
        }

        Long signRecordId = Long.valueOf(redisData.get("signRecordId").toString());
        SignRecord signRecord = signRecordRepository.findById(signRecordId)
                .orElseThrow(() -> new RuntimeException("签署记录不存在"));

        Map<String, Object> result = new HashMap<>();
        result.put("projectId", redisData.get("projectId"));
        result.put("userId", redisData.get("userId"));
        result.put("fileId", redisData.get("fileId"));
        result.put("metaCode", redisData.get("metaCode"));
        result.put("status", signRecord.getStatus().getDescription());

        if (signRecord.getStatus() != SignRecord.SignStatus.UNSCANNED) {
            redisTemplate.delete(token);
        }

        return result;
    }

    public Map<String, Object> confirmSign(String token, String signatureBase64) {
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("无效的token");
        }

        Map<String, Object> redisData = (Map<String, Object>) redisTemplate.opsForValue().get(token);
        if (redisData == null) {
            throw new RuntimeException("token已过期或不存在");
        }

        Long signRecordId = Long.valueOf(redisData.get("signRecordId").toString());
        SignRecord signRecord = signRecordRepository.findById(signRecordId)
                .orElseThrow(() -> new RuntimeException("签署记录不存在"));

        if (signRecord.getStatus() == SignRecord.SignStatus.SIGNED) {
            throw new RuntimeException("该签署请求已完成");
        }

        signRecord.setStatus(SignRecord.SignStatus.SIGNED);
        signRecord.setSignatureBase64(signatureBase64);
        signRecordRepository.save(signRecord);

        redisTemplate.delete(token);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "签署成功");
        result.put("status", SignRecord.SignStatus.SIGNED.getDescription());
        result.put("signatureBase64", signatureBase64);
        result.put("signRecordId", signRecordId);

        return result;
    }
}