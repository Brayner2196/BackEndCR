package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.GastoRegistrado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface GastoRegistradoRepository extends JpaRepository<GastoRegistrado, Long> {

    List<GastoRegistrado> findByPresupuestoIdOrderByFechaDescCreadoEnDesc(Long presupuestoId);

    List<GastoRegistrado> findByCategoriaIdOrderByFechaDescCreadoEnDesc(Long categoriaId);

    @Query("SELECT COALESCE(SUM(g.monto), 0) FROM GastoRegistrado g WHERE g.categoriaId = :categoriaId")
    BigDecimal sumarMontoPorCategoria(Long categoriaId);

    @Query("SELECT COALESCE(SUM(g.monto), 0) FROM GastoRegistrado g WHERE g.presupuestoId = :presupuestoId")
    BigDecimal sumarMontoPorPresupuesto(Long presupuestoId);

    void deleteByCategoriaId(Long categoriaId);
}
