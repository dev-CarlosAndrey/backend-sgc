package br.com.fpo.sgc.domain.brand;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    boolean existsByNameIgnoreCase(String name);
    Page<Brand> findByActiveTrue(Pageable pageable);
    Optional<Brand> findByIdAndActiveTrue(Long id);
}