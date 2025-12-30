package com.example.loanova.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Role extends BaseEntity {

    @Column(name = "role_name", nullable = false, unique = true, length = 15, updatable = false)
    private String roleName;

    @Column(name = "role_description", nullable = false, length = 255)
    private String roleDescription;
}
