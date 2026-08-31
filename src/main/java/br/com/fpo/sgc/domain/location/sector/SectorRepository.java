package br.com.fpo.sgc.domain.location.sector;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Long> {
    Page<Sector> findByActiveTrue(Pageable pageable);
    Page<Sector> findByBuildingIdAndActiveTrue(Long buildingId, Pageable pageable);
    long countByBuildingId(Long buildingId);
    long countByBuildingIdAndActiveTrue(Long buildingId);
}