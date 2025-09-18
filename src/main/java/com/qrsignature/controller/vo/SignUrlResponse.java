package com.qrsignature.controller.vo;

import lombok.Data;

@Data
public class SignUrlResponse {
    private String signUrl;
    private String token;
    private String status;
}