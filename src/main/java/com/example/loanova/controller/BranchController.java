package com.example.loanova.controller;

import com.example.loanova.base.ApiResponse;
import com.example.loanova.dto.request.BranchRequest;
import com.example.loanova.dto.response.BranchResponse;
import com.example.loanova.service.BranchService;
import com.example.loanova.util.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * BRANCH CONTROLLER - REST API untuk manage cabang
 * 
 * Base URL: /api/branches
 * 
 * Authorization:
 * - Semua endpoint hanya bisa diakses oleh SUPERADMIN
 * - Pakai @PreAuthorize untuk check role
 * - Spring Security auto return 403 Forbidden kalau role tidak sesuai
 * 
 * Endpoints:
 * - GET    /api/branches       - Get all branches
 * - POST   /api/branches       - Create branch
 * - PUT    /api/branches/{id}  - Update branch
 * - DELETE /api/branches/{id}  - Delete branch
 */
@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;


    /**
     * GET ALL BRANCHES
     * 
     * Authorization: SUPERADMIN only
     * 
     * @PreAuthorize("hasRole('SUPERADMIN')"):
     * - Check apakah user punya role SUPERADMIN
     * - Spring Security auto tambah prefix "ROLE_" → check "ROLE_SUPERADMIN"
     * - Kalau tidak punya → return 403 Forbidden
     * 
     * @return List of all branches
     */
    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getAllBranches() {
        List<BranchResponse> branches = branchService.getAllBranches();
        return ResponseUtil.ok(branches, "Berhasil mengambil daftar cabang");
    }

    /**
     * CREATE BRANCH
     * 
     * Authorization: SUPERADMIN only
     * Request Body: BranchRequest dengan validation
     * 
     * @param request Branch data (name, address, dll)
     * @return Created branch data
     */
    @PreAuthorize("hasRole('SUPERADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<BranchResponse>> createBranch(
            @Valid @RequestBody BranchRequest request) {
        BranchResponse branch = branchService.createBranch(request);
        return ResponseUtil.created(branch, "Berhasil membuat branch baru");
    }

    /**
     * UPDATE BRANCH
     * 
     * Authorization: SUPERADMIN only
     * Path Variable: Branch ID
     * Request Body: BranchRequest dengan validation
     * 
     * @param id Branch ID yang mau di-update
     * @param request Branch data baru
     * @return Updated branch data
     */
    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchResponse>> updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody BranchRequest request) {
        BranchResponse branch = branchService.updateBranch(id, request);
        return ResponseUtil.ok(branch, "Berhasil memperbarui branch");
    }

    /**
     * DELETE BRANCH
     * 
     * Authorization: SUPERADMIN only
     * Path Variable: Branch ID
     * 
     * Note: Bisa implement soft delete (set deletedAt) atau hard delete (hapus dari DB)
     * 
     * @param id Branch ID yang mau di-delete
     * @return Success message
     */
    @PreAuthorize("hasRole('SUPERADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(
            @PathVariable Long id) {
        branchService.deleteBranch(id);
        return ResponseUtil.ok(null, "Berhasil menghapus branch");
    }
}
