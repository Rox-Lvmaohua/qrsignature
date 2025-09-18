package com.qrsignature.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * @Author: LMH
 * @DATE: 2025/9/18 上午9:17
 * @Description:
 */

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SignUrlRequest {
    private String projectId;
    private String userId;
    private String fileId;
    private String metaCode;
}
