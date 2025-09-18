package com.qrsignature.controller.vo;

import lombok.Data;

@Data
public class SignConfirmResponse {
    private String message;
    private String status;
    private String signatureBase64;
    private String signRecordId;
    private Integer signatureSequence;
}