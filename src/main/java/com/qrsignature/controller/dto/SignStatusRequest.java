package com.qrsignature.controller.dto;

import lombok.Data;

@Data
public class SignStatusRequest {
    private String projectId;
    private String userId;
    private String fileId;
}