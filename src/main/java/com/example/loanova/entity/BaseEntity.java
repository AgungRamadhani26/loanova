package com.example.loanova.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * BASE ENTITY - Parent class untuk semua entity dengan audit fields
 */
@MappedSuperclass
@SQLRestriction("deleted_at IS NULL")
@Data
public abstract class BaseEntity {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @Column(name = "created_at", updatable = false)
   private LocalDateTime createdAt;

   @Column(name = "updated_at")
   private LocalDateTime updatedAt;

   @Column(name = "deleted_at")
   private LocalDateTime deletedAt;

   @PrePersist
   protected void onCreate() {
      createdAt = LocalDateTime.now();
   }

   @PreUpdate
   protected void onUpdate() {
      updatedAt = LocalDateTime.now();
   }

   /**
    * Soft delete - menandai entity sebagai deleted tanpa menghapus dari database
    */
   public void softDelete() {
      this.deletedAt = LocalDateTime.now();
   }

   /**
    * Restore entity yang sudah di-soft delete
    */
   public void restore() {
      this.deletedAt = null;
   }

   /**
    * Check apakah entity sudah di-delete
    * 
    * @return true jika sudah deleted, false jika masih active
    */
   public boolean isDeleted() {
      return deletedAt != null;
   }
}
