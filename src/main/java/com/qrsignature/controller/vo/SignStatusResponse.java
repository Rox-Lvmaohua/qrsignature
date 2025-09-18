package com.qrsignature.controller.vo;

import lombok.Data;

/**
 * @author Administrator
 */
@Data
public class SignStatusResponse {
    private String signRecordId;
    private String status;
    private String signatureBase64;
}