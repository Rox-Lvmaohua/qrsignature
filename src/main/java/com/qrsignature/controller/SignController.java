package com.qrsignature.controller;

import com.qrsignature.controller.dto.SignConfirmRequest;
import com.qrsignature.controller.dto.SignStatusRequest;
import com.qrsignature.controller.dto.SignUrlRequest;
import com.qrsignature.controller.vo.SignConfirmResponse;
import com.qrsignature.controller.vo.SignStatusResponse;
import com.qrsignature.controller.vo.SignUrlResponse;
import com.qrsignature.service.SignService;
import com.qrsignature.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sign")
@CrossOrigin(origins = "*")
public class SignController {

    @Autowired
    private SignService signService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/url")
    public ResponseEntity<?> generateSignUrl(@RequestBody SignUrlRequest request) {
        try {
            SignUrlResponse result = signService.generateSignUrl(
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
    public ResponseEntity<?> checkSignStatus(@RequestParam String projectId,
                                           @RequestParam String userId,
                                           @RequestParam String fileId) {
        try {
            SignStatusResponse result = signService.checkSignStatus(projectId, userId, fileId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "检查签署状态失败",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmSign(@RequestHeader("Authorization") String authorization,
                                       @RequestBody SignConfirmRequest request) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "认证失败",
                        "message", "缺少Bearer Token"
                ));
            }

            String token = authorization.substring(7);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "认证失败",
                        "message", "无效的Token"
                ));
            }

            if (request.getSignatureBase64() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "参数错误",
                        "message", "signatureBase64是必需参数"
                ));
            }

            SignConfirmResponse result = signService.confirmSign(token, request.getSignatureBase64());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "签署确认失败",
                    "message", e.getMessage()
            ));
        }
    }
}