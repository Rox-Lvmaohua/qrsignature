package com.qrsignature.controller;

import com.qrsignature.controller.dto.SignConfirmRequest;
import com.qrsignature.controller.dto.SignStatusRequest;
import com.qrsignature.controller.dto.SignUrlRequest;
import com.qrsignature.controller.vo.*;
import com.qrsignature.service.SignService;
import com.qrsignature.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 */
@RestController
@RequestMapping("/api/sign")
@CrossOrigin(origins = "*")
public class SignController {

    @Autowired
    private SignService signService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/url")
    public ResponseEntity<?> generateSignUrl(@RequestHeader(value = "Authorization", required = false) String authorization,
                                             @RequestBody SignUrlRequest request) {
        try {
            SignUrlResponse result = signService.generateSignUrl(
                    authorization,
                    request.getProjectId(),
                    request.getUserId(),
                    request.getFileId(),
                    request.getMetaCode()
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "生成签署URL失败",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> checkSignStatus(@RequestHeader(value = "Authorization", required = false) String authorization,
                                             @RequestParam String id) {
        try {
            if (authorization == null || authorization.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "参数错误",
                        "message", "未传入token"
                ));
            }
            String token = authorization.substring(7);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "认证失败",
                        "message", "无效的Token"
                ));
            }
            SignStatusResponse result = signService.checkSignStatus(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "检查签署状态失败",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmSign(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @RequestBody SignConfirmRequest request) {
        try {
            String token = null;

            if (authorization != null && authorization.startsWith("Bearer ")) {
                token = authorization.substring(7);
            }

            if (token == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "认证失败",
                        "message", "无效的Token"
                ));
            }

            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "认证失败",
                        "message", "无效的Token"
                ));
            }

            if (!StringUtils.hasText(request.getSignatureBase64()) && !StringUtils.hasText(request.getUserSignatureId())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "参数错误",
                        "message", "signatureBase64或userSignatureId是必需参数"
                ));
            }

            SignConfirmResponse result = signService.confirmSign(token, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "签署确认失败",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/getSign")
    public ResponseEntity<?> getSign(@RequestParam(required = false) String projectId,
                                           @RequestParam String userId,
                                           @RequestParam String fileId) {
        try {

            String result = signService.getBase64Signature(projectId, userId, fileId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "签署确认失败",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取用户历史签名
     */
    @GetMapping("/history")
    public ResponseEntity<?> getUserSignHistory(@RequestParam String userId) {
        try {
            SignHistoryResponse result = signService.getUserSignHistory(userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "获取用户签名历史失败",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取用户签名列表
     */
    @GetMapping("/user-signatures")
    public ResponseEntity<?> getUserSignatures(@RequestParam String userId) {
        try {
            List<UserSignatureVO> result = signService.getUserSignatures(userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "获取用户签名列表失败",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 删除签名记录
     */
    @PostMapping("/delete-record")
    public ResponseEntity<?> deleteSignRecord(@RequestParam String signRecordId) {
        try {
            signService.deleteSignRecord(signRecordId);
            return ResponseEntity.ok(Map.of(
                    "message", "签名记录删除成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "删除签名记录失败",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 删除用户签名
     */
    @PostMapping("/delete-user-signature")
    public ResponseEntity<?> deleteUserSignature(@RequestParam String userId, @RequestParam String signatureId) {
        try {
            signService.deleteUserSignature(userId, signatureId);
            return ResponseEntity.ok(Map.of(
                    "message", "用户签名删除成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "删除用户签名失败",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取签名图片
     */
    @GetMapping("/signature-image")
    public ResponseEntity<?> getSignatureImage(@RequestParam String signRecordId) {
        try {
            String result = signService.getSignatureImage(signRecordId);
            return ResponseEntity.ok(Map.of(
                    "signatureBase64", result
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "获取签名图片失败",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取token中的用户信息
     */
    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo(@RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "认证失败",
                        "message", "无效的Token格式"
                ));
            }

            String token = authorization.substring(7);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "认证失败",
                        "message", "无效的Token"
                ));
            }

            Map<String, Object> claims = jwtUtil.extractClaims(token);
            return ResponseEntity.ok(Map.of(
                    "userId", claims.get("userId"),
                    "projectId", claims.get("projectId"),
                    "fileId", claims.get("fileId"),
                    "metaCode", claims.get("metaCode")
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "获取用户信息失败",
                    "message", e.getMessage()
            ));
        }
    }
}