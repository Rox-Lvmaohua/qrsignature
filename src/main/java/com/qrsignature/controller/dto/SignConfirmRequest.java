package com.qrsignature.controller.dto;

import lombok.Data;

/**
 * @author Administrator
 */
@Data
public class SignConfirmRequest {
    private String token;
    private String signatureBase64;
}