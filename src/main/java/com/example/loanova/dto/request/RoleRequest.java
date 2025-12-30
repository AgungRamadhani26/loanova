package com.example.loanova.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleRequest {
    @NotBlank(message = "Role name wajib diisi")
    @Size(max = 15, message = "Role name maksimal 15 karakter")
    private String roleName;

    @NotBlank(message = "Role description wajib diisi")
    @Size(max = 255, message = "Role description maksimal 255 karakter")
    private String roleDescription;
}
