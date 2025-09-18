package com.qrsignature.controller.vo;

import lombok.Data;

@Data
public class SignStatusResponse {
    private String projectId;
    private String userId;
    private String fileId;
    private String metaCode;
    private String status;
    private String signatureBase64;
}