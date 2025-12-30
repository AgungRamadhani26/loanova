package com.example.loanova.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "branches")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Branch extends BaseEntity {

    @Column(name = "branch_code", length = 20, nullable = false, unique = true)
    private String branchCode;

    @Column(name = "branch_name", length = 100, nullable = false, unique = true)
    private String branchName;

    @Column(name = "address", nullable = false)
    private String address;
}
