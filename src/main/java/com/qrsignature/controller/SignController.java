package com.qrsignature.controller;

import com.qrsignature.controller.dto.SignUrlRequest;
import com.qrsignature.service.SignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/api/sign")
@CrossOrigin(origins = "*")
public class SignController {

    @Autowired
    private SignService signService;

    @PostMapping("/url")
    @ResponseBody
    public ResponseEntity<?> generateSignUrl(@RequestBody SignUrlRequest request) {
        try {
            String projectId = request.getProjectId();
            String userId = request.getUserId();
            String fileId = request.getFileId();
            String metaCode = request.getMetaCode();


            Map<String, Object> result = signService.generateSignUrl(projectId, userId, fileId, metaCode);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "生成签署URL失败",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{token}")
    @ResponseBody
    public ResponseEntity<?> validateToken(@PathVariable String token) {
        try {
            Map<String, Object> result = signService.validateToken(token);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "token验证失败",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/sign")
    public String signPage(String token, Model model) {
        // 处理逻辑
        model.addAttribute("token", token);
        // 返回正确的视图名称
        return "sign";
    }

    @PostMapping("/confirm")
    @ResponseBody
    public ResponseEntity<?> confirmSign(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            String signatureBase64 = request.get("signatureBase64");

            if (token == null || signatureBase64 == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "缺少必要参数",
                        "message", "token和signatureBase64都是必需参数"
                ));
            }

            Map<String, Object> result = signService.confirmSign(token, signatureBase64);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "签署确认失败",
                    "message", e.getMessage()
            ));
        }
    }
}