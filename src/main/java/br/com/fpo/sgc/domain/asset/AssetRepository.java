package br.com.fpo.sgc.domain.asset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
    boolean existsByTagNumber(String tagNumber);
    Optional<Asset> findByTagNumber(String tagNumber);
    // Usado pela abertura de OS via WhatsApp (RF15): o tombamento vem em texto livre do usuário.
    Optional<Asset> findByTagNumberIgnoreCaseAndActiveTrue(String tagNumber);
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

    @Query("SELECT COUNT(a) FROM Asset a " +
            "WHERE a.active = true " +
            "AND a.acquisitionDate IS NOT NULL AND a.acquisitionValue IS NOT NULL " +
            "AND (:companyId IS NULL OR a.sector.building.company.id = :companyId)")
    long countDepreciatingByCompanyId(@Param("companyId") Long companyId);

    // categoryId/categoryName nulos = bens sem categoria.
    @Query("SELECT c.id, c.name, a.status, COUNT(a) FROM Asset a LEFT JOIN a.category c " +
            "WHERE a.active = true " +
            "AND (:companyId IS NULL OR a.sector.building.company.id = :companyId) " +
            "GROUP BY c.id, c.name, a.status")
    List<Object[]> countByCategoryAndStatus(@Param("companyId") Long companyId);

    @Query("SELECT a.status, COUNT(a) FROM Asset a " +
            "WHERE a.active = true " +
            "AND (:companyId IS NULL OR a.sector.building.company.id = :companyId) " +
            "GROUP BY a.status")
    List<Object[]> countByStatusAndCompanyId(@Param("companyId") Long companyId);

    // Bens ativos com setor e categoria carregados, para o relatório de patrimônio.
    @Query("SELECT a FROM Asset a JOIN FETCH a.sector LEFT JOIN FETCH a.category " +
            "WHERE a.active = true " +
            "AND (:companyId IS NULL OR a.sector.building.company.id = :companyId)")
    List<Asset> findForPatrimony(@Param("companyId") Long companyId);

    @Query("SELECT a FROM Asset a JOIN FETCH a.sector LEFT JOIN FETCH a.category " +
            "WHERE a.active = true AND a.conservationState IN :states " +
            "AND (:companyId IS NULL OR a.sector.building.company.id = :companyId)")
    List<Asset> findByConservationStates(@Param("states") Collection<AssetConservationState> states,
                                         @Param("companyId") Long companyId);
}
