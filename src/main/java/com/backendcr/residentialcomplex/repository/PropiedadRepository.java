package com.backendcr.residentialcomplex.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.backendcr.residentialcomplex.entity.Propiedad;

public interface PropiedadRepository extends JpaRepository<Propiedad, Long> {

    Optional<Propiedad> findByTipoIdAndIdentificadorAndParentId(
            Long tipoId, String identificador, Long parentId);

    List<Propiedad> findByParentIdIsNull();

    List<Propiedad> findByParentId(Long parentId);
    
    @Query("Select p from Propiedad p join TipoPropiedad t on p.tipoId = t.id where t.esFacturable=true")
    List<Propiedad> findByTipoIdIsFacturable();
    
    @Query("Select COUNT(p) from Propiedad p join TipoPropiedad t on p.tipoId = t.id where t.esFacturable=true")
    Integer countPropiedadesIsFacturable();
    
}
