package com.qrsignature.controller.dto;

import lombok.Data;

/**
 * @Author: LMH
 * @DATE: 2025/9/18 上午9:17
 * @Description:
 */

@Data
public class SignUrlRequest {
    private String projectId;
    private String userId;
    private String fileId;
    private String metaCode;
}
