package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.ResidentialUnitDTO;
import com.backendcr.residentialcomplex.entity.ResidentialComplex;
import com.backendcr.residentialcomplex.entity.ResidentialUnit;
import com.backendcr.residentialcomplex.exception.ResourceNotFoundException;
import com.backendcr.residentialcomplex.multitenancy.TenantContext;
import com.backendcr.residentialcomplex.repository.ResidentialComplexRepository;
import com.backendcr.residentialcomplex.repository.ResidentialUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResidentialUnitService {
    
    private final ResidentialUnitRepository repository;
    private final ResidentialComplexRepository complexRepository;
    
    @Transactional(readOnly = true)
    public List<ResidentialUnitDTO> findAll() {
        String tenantId = TenantContext.getCurrentTenant();
        return repository.findByTenantId(tenantId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public ResidentialUnitDTO findById(Long id) {
        String tenantId = TenantContext.getCurrentTenant();
        ResidentialUnit unit = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ResidentialUnit", "id", id));
        return toDTO(unit);
    }
    
    @Transactional(readOnly = true)
    public List<ResidentialUnitDTO> findByComplexId(Long complexId) {
        String tenantId = TenantContext.getCurrentTenant();
        return repository.findByComplexIdAndTenantId(complexId, tenantId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public ResidentialUnitDTO create(ResidentialUnitDTO dto) {
        String tenantId = TenantContext.getCurrentTenant();
        ResidentialComplex complex = complexRepository.findByIdAndTenantId(dto.getComplexId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ResidentialComplex", "id", dto.getComplexId()));
        
        ResidentialUnit unit = toEntity(dto, complex);
        ResidentialUnit savedUnit = repository.save(unit);
        return toDTO(savedUnit);
    }
    
    @Transactional
    public ResidentialUnitDTO update(Long id, ResidentialUnitDTO dto) {
        String tenantId = TenantContext.getCurrentTenant();
        ResidentialUnit unit = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ResidentialUnit", "id", id));
        
        if (dto.getComplexId() != null && !dto.getComplexId().equals(unit.getComplex().getId())) {
            ResidentialComplex complex = complexRepository.findByIdAndTenantId(dto.getComplexId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("ResidentialComplex", "id", dto.getComplexId()));
            unit.setComplex(complex);
        }
        
        unit.setUnitNumber(dto.getUnitNumber());
        unit.setUnitType(dto.getUnitType());
        unit.setFloorNumber(dto.getFloorNumber());
        unit.setAreaSqm(dto.getAreaSqm());
        unit.setIsOccupied(dto.getIsOccupied());
        
        ResidentialUnit updatedUnit = repository.save(unit);
        return toDTO(updatedUnit);
    }
    
    @Transactional
    public void delete(Long id) {
        String tenantId = TenantContext.getCurrentTenant();
        ResidentialUnit unit = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ResidentialUnit", "id", id));
        repository.delete(unit);
    }
    
    private ResidentialUnitDTO toDTO(ResidentialUnit unit) {
        return ResidentialUnitDTO.builder()
                .id(unit.getId())
                .complexId(unit.getComplex().getId())
                .unitNumber(unit.getUnitNumber())
                .unitType(unit.getUnitType())
                .floorNumber(unit.getFloorNumber())
                .areaSqm(unit.getAreaSqm())
                .isOccupied(unit.getIsOccupied())
                .tenantId(unit.getTenantId())
                .build();
    }
    
    private ResidentialUnit toEntity(ResidentialUnitDTO dto, ResidentialComplex complex) {
        return ResidentialUnit.builder()
                .complex(complex)
                .unitNumber(dto.getUnitNumber())
                .unitType(dto.getUnitType())
                .floorNumber(dto.getFloorNumber())
                .areaSqm(dto.getAreaSqm())
                .isOccupied(dto.getIsOccupied())
                .build();
    }
}
