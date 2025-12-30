package com.example.loanova.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BranchRequest {
    @NotBlank(message = "Branch code wajib diisi")
    @Size(max = 20, message = "Branch code maksimal 20 karakter")
    private String branchCode;

    @NotBlank(message = "Branch name wajib diisi")
    @Size(max = 100, message = "Branch name maksimal 100 karakter")
    private String branchName;

    @NotBlank(message = "Address wajib diisi")
    private String address;
}
