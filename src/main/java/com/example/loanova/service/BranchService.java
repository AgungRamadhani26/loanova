package com.example.loanova.service;

import com.example.loanova.dto.request.BranchRequest;
import com.example.loanova.dto.response.BranchResponse;
import com.example.loanova.entity.Branch;
import com.example.loanova.exception.DuplicateResourceException;
import com.example.loanova.exception.ResourceNotFoundException;
import com.example.loanova.repository.BranchRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BranchService {

    private final BranchRepository branchRepository;

    // Sebenernya tanpa kode dibawah bisa namun kita harus memberikan
    // anotasi @RequiredArgsConstructor sebelum nama kelas BranchService
    // ini merupakah anotasi bawaan lombok
    public BranchService(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    /*
     * Mendapatkan semua Branch yang ada di sistem (auto exclude deleted via @Where)
     */
    public List<BranchResponse> getAllBranches() {
        return branchRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /* Menambahkan Branch baru ke dalam sistem */
    public BranchResponse createBranch(BranchRequest request) {
        /*
         * Pengecekan jika data baru mempunyai branchCode yang sama dengan yang sudah
         * terdaftar pada sistem
         */
        if (branchRepository.existsByBranchCode(request.getBranchCode())) {
            throw new DuplicateResourceException(
                    "Branch code " + request.getBranchCode() + " sudah digunakan");
        }
        Branch branch = Branch.builder()
                .branchCode(request.getBranchCode())
                .branchName(request.getBranchName())
                .address(request.getAddress())
                .build();
        return toResponse(branchRepository.save(branch));
    }

    /* Mengupdate data branch */
    public BranchResponse updateBranch(Long id, BranchRequest request) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maaf, tidak ada data branch dengan id " + id));
        if (!branch.getBranchCode().equals(request.getBranchCode())
                && branchRepository.existsByBranchCode(request.getBranchCode())) {
            throw new DuplicateResourceException(
                    "Branch code " + request.getBranchCode() + " sudah digunakan");
        }
        branch.setBranchCode(request.getBranchCode());
        branch.setBranchName(request.getBranchName());
        branch.setAddress(request.getAddress());
        return toResponse(branchRepository.save(branch));
    }

    /*
     * Soft delete - menandai branch sebagai deleted tanpa menghapus dari database
     */
    public void deleteBranch(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maaf, tidak ada data branch dengan id " + id));
        branch.softDelete();
        branchRepository.save(branch);
    }

    /* Method helper untuk membantu mapping Entity ke DTO */
    private BranchResponse toResponse(Branch branch) {
        return BranchResponse.builder()
                .id(branch.getId())
                .branchCode(branch.getBranchCode())
                .branchName(branch.getBranchName())
                .address(branch.getAddress())
                .build();
    }
}
