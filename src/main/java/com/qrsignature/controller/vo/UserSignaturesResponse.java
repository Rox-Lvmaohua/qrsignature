package com.qrsignature.controller.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户签名历史响应
 */
@Data
public class UserSignaturesResponse {
    
    private String userId;
    private List<SignatureInfo> signatures;
    
    @Data
    public static class SignatureInfo {
        private String id;
        private String signatureBase64;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}