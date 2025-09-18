package com.qrsignature.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * @author Administrator
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SignConfirmRequest {
    private String token;
    private String signatureBase64;
}