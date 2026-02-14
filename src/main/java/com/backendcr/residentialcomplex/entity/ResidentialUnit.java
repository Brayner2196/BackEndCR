package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.multitenancy.TenantAwareEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;

/**
 * Entity representing a residential unit (apartment, house, etc.).
 */
@Entity
@Table(name = "residential_units")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResidentialUnit extends TenantAwareEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complex_id", nullable = false)
    private ResidentialComplex complex;
    
    @NotBlank(message = "Unit number is required")
    @Size(max = 20, message = "Unit number must not exceed 20 characters")
    @Column(name = "unit_number", nullable = false, length = 20)
    private String unitNumber;
    
    @Size(max = 50, message = "Unit type must not exceed 50 characters")
    @Column(name = "unit_type", length = 50)
    private String unitType; // e.g., Apartment, House, Townhouse
    
    @Column(name = "floor_number")
    private Integer floorNumber;
    
    @Column(name = "area_sqm")
    private Double areaSqm;
    
    @Column(name = "is_occupied")
    private Boolean isOccupied;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isOccupied == null) {
            isOccupied = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
