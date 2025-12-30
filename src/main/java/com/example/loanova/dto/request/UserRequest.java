package com.example.loanova.dto.request;

import com.example.loanova.validation.StrongPassword;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Set;

@Data
public class UserRequest {
    @NotBlank(message = "Username wajib diisi")
    @Size(max = 50, message = "Username maksimal 50 karakter")
    private String username;

    @NotBlank(message = "Email wajib diisi")
    @Email(message = "Format email tidak valid")
    private String email;

    @NotBlank(message = "Password wajib diisi")
    @StrongPassword
    private String password;

    // Boleh null untuk customer
    private Long branchId;

    @NotNull(message = "Status aktif wajib diisi")
    private Boolean isActive;

    // Lebih tepat pakai not empty karena set
    @NotEmpty(message = "Role wajib dipilih minimal 1")
    private Set<Long> roleIds;
}
