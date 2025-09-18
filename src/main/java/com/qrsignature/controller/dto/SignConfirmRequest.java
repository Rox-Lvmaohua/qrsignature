package com.qrsignature.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * @author Administrator
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SignConfirmRequest {
    private String signatureId;
    private String signatureBase64;
    private Boolean saveForReuse = false;
    private String signatureName;
    private Boolean setAsDefault = false;
    // 如果使用历史签名，传入签名ID
    private String userSignatureId;
}