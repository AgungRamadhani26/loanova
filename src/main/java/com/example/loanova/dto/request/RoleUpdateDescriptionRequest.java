package com.example.loanova.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleUpdateDescriptionRequest {
    @NotBlank(message = "Role description wajib diisi")
    @Size(max = 255, message = "Role description maksimal 255 karakter")
    private String roleDescription;
}
