package br.com.fpo.sgc.domain.asset.category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetCategoryRepository extends JpaRepository<AssetCategory, Long> {
    Page<AssetCategory> findByActiveTrue(Pageable pageable);
    boolean existsByNameIgnoreCase(String name);

    // Listagem com busca e filtros opcionais (null = sem filtro). `q` já chega como padrão LIKE
    // montado por SearchTerm.toLikePattern.
    @Query("SELECT c FROM AssetCategory c " +
            "WHERE c.active = true " +
            "AND (:q IS NULL " +
            "OR LOWER(c.name) LIKE :q ESCAPE '!' " +
            "OR LOWER(c.description) LIKE :q ESCAPE '!')")
    Page<AssetCategory> search(@Param("q") String q, Pageable pageable);
}
