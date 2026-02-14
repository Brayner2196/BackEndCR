package com.backendcr.residentialcomplex.dto;

import jakarta.validation.constraints.Email;
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
public class ResidentDTO {
    
    private Long id;
    
    @NotNull(message = "Unit ID is required")
    private Long unitId;
    
    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;
    
    @Size(max = 50, message = "Document number must not exceed 50 characters")
    private String documentNumber;
    
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;
    
    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;
    
    private Boolean isOwner;
    
    private String tenantId;
}
