package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.ResidentialComplexDTO;
import com.backendcr.residentialcomplex.entity.ResidentialComplex;
import com.backendcr.residentialcomplex.exception.ResourceNotFoundException;
import com.backendcr.residentialcomplex.multitenancy.TenantContext;
import com.backendcr.residentialcomplex.repository.ResidentialComplexRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResidentialComplexService {
    
    private final ResidentialComplexRepository repository;
    
    @Transactional(readOnly = true)
    public List<ResidentialComplexDTO> findAll() {
        String tenantId = TenantContext.getCurrentTenant();
        return repository.findByTenantId(tenantId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public ResidentialComplexDTO findById(Long id) {
        String tenantId = TenantContext.getCurrentTenant();
        ResidentialComplex complex = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ResidentialComplex", "id", id));
        return toDTO(complex);
    }
    
    @Transactional
    public ResidentialComplexDTO create(ResidentialComplexDTO dto) {
        ResidentialComplex complex = toEntity(dto);
        ResidentialComplex savedComplex = repository.save(complex);
        return toDTO(savedComplex);
    }
    
    @Transactional
    public ResidentialComplexDTO update(Long id, ResidentialComplexDTO dto) {
        String tenantId = TenantContext.getCurrentTenant();
        ResidentialComplex complex = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ResidentialComplex", "id", id));
        
        complex.setName(dto.getName());
        complex.setAddress(dto.getAddress());
        complex.setPhone(dto.getPhone());
        complex.setEmail(dto.getEmail());
        complex.setTotalUnits(dto.getTotalUnits());
        
        ResidentialComplex updatedComplex = repository.save(complex);
        return toDTO(updatedComplex);
    }
    
    @Transactional
    public void delete(Long id) {
        String tenantId = TenantContext.getCurrentTenant();
        ResidentialComplex complex = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ResidentialComplex", "id", id));
        repository.delete(complex);
    }
    
    @Transactional(readOnly = true)
    public List<ResidentialComplexDTO> searchByName(String name) {
        String tenantId = TenantContext.getCurrentTenant();
        return repository.findByNameContainingIgnoreCaseAndTenantId(name, tenantId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    private ResidentialComplexDTO toDTO(ResidentialComplex complex) {
        return ResidentialComplexDTO.builder()
                .id(complex.getId())
                .name(complex.getName())
                .address(complex.getAddress())
                .phone(complex.getPhone())
                .email(complex.getEmail())
                .totalUnits(complex.getTotalUnits())
                .tenantId(complex.getTenantId())
                .build();
    }
    
    private ResidentialComplex toEntity(ResidentialComplexDTO dto) {
        return ResidentialComplex.builder()
                .name(dto.getName())
                .address(dto.getAddress())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .totalUnits(dto.getTotalUnits())
                .build();
    }
}
