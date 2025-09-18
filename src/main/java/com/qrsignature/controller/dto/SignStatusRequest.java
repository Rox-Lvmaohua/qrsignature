package com.qrsignature.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SignStatusRequest {
    private String projectId;
    private String userId;
    private String fileId;
}