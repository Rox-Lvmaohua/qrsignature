package com.qrsignature.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.qrsignature.controller.dto.SignConfirmRequest;
import com.qrsignature.controller.vo.SignConfirmResponse;
import com.qrsignature.controller.vo.SignStatusResponse;
import com.qrsignature.controller.vo.SignUrlResponse;
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
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class SignService {

    @Autowired
    private SignRecordRepository signRecordRepository;

    @Autowired
    private UserSignatureRepository userSignatureRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 使用Guava Cache实现本地缓存，设置15分钟过期时间
    private final Cache<String, Map<String, Object>> tokenCache = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    // 状态查询缓存，缓存5分钟
    private final Cache<String, SignStatusResponse> statusCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

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

        tokenCache.put(token, redisData);
        token = "Bearer " + token;

        SignUrlResponse response = new SignUrlResponse();
        response.setSignUrl(signUrl);
        response.setToken(token);
        response.setStatus(signRecord.getStatus().getDescription());
        response.setSignatureSequence(nextSequence);
        response.setSignRecordId(signRecord.getId());

        return response;
    }

    public SignStatusResponse checkSignStatus(String signRecordId) {
        try {
            // 使用Guava Cache的原子性get操作，确保只有一个线程会执行数据库查询
            return statusCache.get(signRecordId, () -> {
                Optional<SignRecord> signRecord = signRecordRepository.findById(signRecordId);

                if (signRecord.isEmpty()) {
                    throw new RuntimeException("签署记录不存在");
                }

                SignRecord record = signRecord.get();
                SignStatusResponse response = new SignStatusResponse();
                response.setSignRecordId(signRecordId);
                response.setStatus(record.getStatus().getDescription());

                if (record.getStatus() == SignRecord.SignStatus.SIGNED) {
                    response.setSignatureBase64(record.getSignatureBase64());
                }

                return response;
            });
        } catch (ExecutionException e) {
            throw new RuntimeException("查询签署状态失败", e);
        }
    }

    public SignConfirmResponse confirmSign(String token, SignConfirmRequest request) {
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("无效的token");
        }

        Map<String, Object> cacheData = tokenCache.getIfPresent(token);
        if (cacheData == null) {
            throw new RuntimeException("token已过期或不存在");
        }

        String signRecordId = cacheData.get("signRecordId").toString();
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
            }
        } else if (request.getSaveForReuse() != null && request.getSaveForReuse()) {
            // 保存用户签名以便重用
            saveUserSignature(signRecord.getUserId(), signatureBase64);
        }

        signRecord.setStatus(SignRecord.SignStatus.SIGNED);
        signRecord.setSignatureBase64(signatureBase64);
        signRecordRepository.save(signRecord);

        statusCache.invalidate(signRecordId);
        tokenCache.invalidate(token);

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
    private void saveUserSignature(String userId, String signatureBase64) {
        UserSignature userSignature = new UserSignature(userId, signatureBase64);
        userSignatureRepository.save(userSignature);
    }
}