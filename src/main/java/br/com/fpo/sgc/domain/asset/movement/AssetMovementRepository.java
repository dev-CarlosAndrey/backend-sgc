package br.com.fpo.sgc.domain.asset.movement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AssetMovementRepository extends JpaRepository<AssetMovement, Long> {
    Page<AssetMovement> findByActiveTrueOrderByMovementDateDesc(Pageable pageable);
    Page<AssetMovement> findByAssetIdAndActiveTrueOrderByMovementDateDesc(Long assetId, Pageable pageable);

    // Company pelo setor de destino.
    @Query("SELECT m FROM AssetMovement m JOIN FETCH m.asset JOIN FETCH m.originSector JOIN FETCH m.destinationSector d " +
            "WHERE m.active = true " +
            "AND (:from IS NULL OR m.movementDate >= :from) AND (:to IS NULL OR m.movementDate < :to) " +
            "AND (:assetId IS NULL OR m.asset.id = :assetId) " +
            "AND (:companyId IS NULL OR d.building.company.id = :companyId) " +
            "ORDER BY m.movementDate DESC")
    List<AssetMovement> findForReport(@Param("from") Instant from, @Param("to") Instant to,
                                      @Param("assetId") Long assetId, @Param("companyId") Long companyId);

    // Listagem com busca e filtros opcionais (null = sem filtro). `q` já chega como padrão LIKE
    // montado por SearchTerm.toLikePattern.
    @Query("SELECT m FROM AssetMovement m JOIN m.asset a LEFT JOIN m.originSector o JOIN m.destinationSector d " +
            "WHERE m.active = true " +
            "AND (:destinationSectorId IS NULL OR d.id = :destinationSectorId) " +
            "AND (:q IS NULL " +
            "OR LOWER(a.tagNumber) LIKE :q ESCAPE '!' " +
            "OR LOWER(a.name) LIKE :q ESCAPE '!' " +
            "OR LOWER(o.name) LIKE :q ESCAPE '!' " +
            "OR LOWER(d.name) LIKE :q ESCAPE '!')")
    Page<AssetMovement> search(@Param("q") String q,
                               @Param("destinationSectorId") Long destinationSectorId,
                               Pageable pageable);
}
