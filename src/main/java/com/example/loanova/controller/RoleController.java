package com.example.loanova.controller;

import com.example.loanova.base.ApiResponse;
import com.example.loanova.dto.request.RoleRequest;
import com.example.loanova.dto.request.RoleUpdateDescriptionRequest;
import com.example.loanova.dto.response.RoleResponse;
import com.example.loanova.service.RoleService;
import com.example.loanova.util.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    // GET ALL ROLES
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        List<RoleResponse> roles = roleService.getAllRoles();
        return ResponseUtil.ok(roles, "Berhasil mengambil daftar role");
    }

    // CREATE ROLE
    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
           @Valid @RequestBody RoleRequest request) {
        RoleResponse role = roleService.createRole(request);
        return ResponseUtil.created(role, "Berhasil membuat role baru");
    }

    //UPDATE ROLE DESCRIPTION
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRoleDescription(
            @PathVariable Long id,
            @Valid @RequestBody RoleUpdateDescriptionRequest request
    ) {
        RoleResponse role = roleService.updateRoleDescription(id, request);
        return ResponseUtil.ok(role, "Berhasil memperbarui deskripsi role");
    }

    // DELETE ROLE
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(
            @PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseUtil.ok(null, "Berhasil menghapus role");
    }
}
