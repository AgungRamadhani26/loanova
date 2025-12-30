package com.example.loanova.service;

import com.example.loanova.dto.request.RoleRequest;
import com.example.loanova.dto.request.RoleUpdateDescriptionRequest;
import com.example.loanova.dto.response.RoleResponse;
import com.example.loanova.entity.Role;
import com.example.loanova.exception.DuplicateResourceException;
import com.example.loanova.exception.ResourceNotFoundException;
import com.example.loanova.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /*
     * Mendapatkan semua Role yang ada di sistem (auto exclude deleted via @Where)
     */
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /* Menambahkan Role baru ke dalam sistem */
    public RoleResponse createRole(RoleRequest request) {
        /*
         * Pengecekan jika data baru mempunyai roleName yang sama dengan yang sudah
         * terdaftar pada sistem
         */
        if (roleRepository.existsByRoleName(request.getRoleName())) {
            throw new DuplicateResourceException(
                    "Role name " + request.getRoleName() + " sudah ada, anda tidak bisa menambahkan role yang sama");
        }
        Role role = Role.builder()
                .roleName(request.getRoleName())
                .roleDescription(request.getRoleDescription())
                .build();
        return toResponse(roleRepository.save(role));
    }

    /* Mengupdate deskripsi data role */
    public RoleResponse updateRoleDescription(Long id, RoleUpdateDescriptionRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role dengan id " + id + " tidak ditemukan"));
        // Hanya update deskripsi
        role.setRoleDescription(request.getRoleDescription());
        return toResponse(roleRepository.save(role));
    }

    /* Soft delete - menandai role sebagai deleted tanpa menghapus dari database */
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maaf, tidak ada data role dengan id " + id));
        role.softDelete();
        roleRepository.save(role);
    }

    /* Method helper untuk membantu mapping Entity ke DTO */
    private RoleResponse toResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .roleName(role.getRoleName())
                .roleDescription(role.getRoleDescription())
                .build();
    }
}
