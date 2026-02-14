package com.backendcr.residentialcomplex.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentialUnitDTO {
    
    private Long id;
    
    @NotNull(message = "Complex ID is required")
    private Long complexId;
    
    @NotBlank(message = "Unit number is required")
    @Size(max = 20, message = "Unit number must not exceed 20 characters")
    private String unitNumber;
    
    @Size(max = 50, message = "Unit type must not exceed 50 characters")
    private String unitType;
    
    private Integer floorNumber;
    
    private Double areaSqm;
    
    private Boolean isOccupied;
    
    private String tenantId;
}
