package com.qrsignature.controller.vo;

import lombok.Data;

@Data
public class SignStatusResponse {
    private String status;
    private String signatureBase64;
    private Integer signatureSequence;
}