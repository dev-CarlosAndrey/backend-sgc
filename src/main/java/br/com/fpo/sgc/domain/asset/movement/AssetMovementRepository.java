package br.com.fpo.sgc.domain.asset.movement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetMovementRepository extends JpaRepository<AssetMovement, Long> {
    Page<AssetMovement> findByActiveTrueOrderByMovementDateDesc(Pageable pageable);
    Page<AssetMovement> findByAssetIdAndActiveTrueOrderByMovementDateDesc(Long assetId, Pageable pageable);
}