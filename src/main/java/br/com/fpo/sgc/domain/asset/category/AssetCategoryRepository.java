package br.com.fpo.sgc.domain.asset.category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetCategoryRepository extends JpaRepository<AssetCategory, Long> {
    Page<AssetCategory> findByActiveTrue(Pageable pageable);
    boolean existsByNameIgnoreCase(String name);
}
