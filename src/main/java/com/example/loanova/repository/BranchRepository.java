package com.example.loanova.repository;

import com.example.loanova.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    boolean existsByBranchCode(String branchCode);
}
