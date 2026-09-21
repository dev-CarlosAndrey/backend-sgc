package br.com.fpo.sgc.domain.asset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
    boolean existsByTagNumber(String tagNumber);
    Optional<Asset> findByTagNumber(String tagNumber);
    Page<Asset> findByActiveTrue(Pageable pageable);
    Page<Asset> findBySectorIdAndActiveTrue(Long sectorId, Pageable pageable);
    Page<Asset> findByStatusAndActiveTrue(AssetStatus status, Pageable pageable);
    Page<Asset> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);
    Page<Asset> findByResponsibleIdAndActiveTrue(Long responsibleId, Pageable pageable);
    @Query("SELECT COUNT(a) FROM Asset a " +
            "WHERE a.active = true " +
            "AND (:companyId IS NULL OR a.sector.building.company.id = :companyId)")
    long countByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT a.sector.building.company.companyName, COUNT(a) " +
            "FROM Asset a " +
            "WHERE a.active = true " +
            "AND (:companyId IS NULL OR a.sector.building.company.id = :companyId) " +
            "GROUP BY a.sector.building.company.companyName")
    List<Object[]> countAssetsByCompany(@Param("companyId") Long companyId);
}
