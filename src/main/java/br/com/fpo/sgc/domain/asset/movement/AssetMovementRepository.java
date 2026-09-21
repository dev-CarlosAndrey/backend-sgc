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
}
