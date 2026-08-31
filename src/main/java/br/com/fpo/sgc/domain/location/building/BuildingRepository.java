package br.com.fpo.sgc.domain.location.building;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BuildingRepository extends JpaRepository<Building, Long> {
    Page<Building> findByActiveTrue(Pageable pageable);
    Page<Building> findByCompanyIdAndActiveTrue(Long companyId, Pageable pageable);
    Optional<Building> findByIdAndActiveTrue(Long id);
    long countByCompanyIdAndActiveTrue(Long companyId);
}