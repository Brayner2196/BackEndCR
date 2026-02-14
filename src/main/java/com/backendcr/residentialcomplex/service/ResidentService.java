package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.ResidentDTO;
import com.backendcr.residentialcomplex.entity.Resident;
import com.backendcr.residentialcomplex.entity.ResidentialUnit;
import com.backendcr.residentialcomplex.exception.ResourceNotFoundException;
import com.backendcr.residentialcomplex.multitenancy.TenantContext;
import com.backendcr.residentialcomplex.repository.ResidentRepository;
import com.backendcr.residentialcomplex.repository.ResidentialUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResidentService {
    
    private final ResidentRepository repository;
    private final ResidentialUnitRepository unitRepository;
    
    @Transactional(readOnly = true)
    public List<ResidentDTO> findAll() {
        String tenantId = TenantContext.getCurrentTenant();
        return repository.findByTenantId(tenantId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public ResidentDTO findById(Long id) {
        String tenantId = TenantContext.getCurrentTenant();
        Resident resident = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "id", id));
        return toDTO(resident);
    }
    
    @Transactional(readOnly = true)
    public List<ResidentDTO> findByUnitId(Long unitId) {
        String tenantId = TenantContext.getCurrentTenant();
        return repository.findByUnitIdAndTenantId(unitId, tenantId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public ResidentDTO create(ResidentDTO dto) {
        String tenantId = TenantContext.getCurrentTenant();
        ResidentialUnit unit = unitRepository.findByIdAndTenantId(dto.getUnitId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ResidentialUnit", "id", dto.getUnitId()));
        
        Resident resident = toEntity(dto, unit);
        Resident savedResident = repository.save(resident);
        return toDTO(savedResident);
    }
    
    @Transactional
    public ResidentDTO update(Long id, ResidentDTO dto) {
        String tenantId = TenantContext.getCurrentTenant();
        Resident resident = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "id", id));
        
        if (dto.getUnitId() != null && !dto.getUnitId().equals(resident.getUnit().getId())) {
            ResidentialUnit unit = unitRepository.findByIdAndTenantId(dto.getUnitId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("ResidentialUnit", "id", dto.getUnitId()));
            resident.setUnit(unit);
        }
        
        resident.setFirstName(dto.getFirstName());
        resident.setLastName(dto.getLastName());
        resident.setDocumentNumber(dto.getDocumentNumber());
        resident.setPhone(dto.getPhone());
        resident.setEmail(dto.getEmail());
        resident.setIsOwner(dto.getIsOwner());
        
        Resident updatedResident = repository.save(resident);
        return toDTO(updatedResident);
    }
    
    @Transactional
    public void delete(Long id) {
        String tenantId = TenantContext.getCurrentTenant();
        Resident resident = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "id", id));
        repository.delete(resident);
    }
    
    @Transactional(readOnly = true)
    public List<ResidentDTO> searchByLastName(String lastName) {
        String tenantId = TenantContext.getCurrentTenant();
        return repository.findByLastNameContainingIgnoreCaseAndTenantId(lastName, tenantId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    private ResidentDTO toDTO(Resident resident) {
        return ResidentDTO.builder()
                .id(resident.getId())
                .unitId(resident.getUnit().getId())
                .firstName(resident.getFirstName())
                .lastName(resident.getLastName())
                .documentNumber(resident.getDocumentNumber())
                .phone(resident.getPhone())
                .email(resident.getEmail())
                .isOwner(resident.getIsOwner())
                .tenantId(resident.getTenantId())
                .build();
    }
    
    private Resident toEntity(ResidentDTO dto, ResidentialUnit unit) {
        return Resident.builder()
                .unit(unit)
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .documentNumber(dto.getDocumentNumber())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .isOwner(dto.getIsOwner())
                .build();
    }
}
