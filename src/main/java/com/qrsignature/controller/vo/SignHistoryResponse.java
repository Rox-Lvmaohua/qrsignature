package com.qrsignature.controller.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SignHistoryResponse {
    private List<SignRecordVO> signRecords;
    private List<UserSignatureVO> userSignatures;

    @Data
    public static class SignRecordVO {
        private String id;
        private String projectId;
        private String userId;
        private String fileId;
        private String metaCode;
        private String status;
        private String signatureBase64;
        private Integer signatureSequence;
        private String userSignatureId;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createTime;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updateTime;
    }
}