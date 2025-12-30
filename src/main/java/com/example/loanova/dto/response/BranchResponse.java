package com.example.loanova.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BranchResponse {
    private Long id;
    private String branchCode;
    private String branchName;
    private String address;
}
