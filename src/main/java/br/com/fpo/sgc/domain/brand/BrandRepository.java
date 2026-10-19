package br.com.fpo.sgc.domain.brand;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    boolean existsByNameIgnoreCase(String name);
    Page<Brand> findByActiveTrue(Pageable pageable);
    Optional<Brand> findByIdAndActiveTrue(Long id);

    // Listagem com busca e filtros opcionais (null = sem filtro). `q` já chega como padrão LIKE
    // montado por SearchTerm.toLikePattern.
    @Query("SELECT b FROM Brand b " +
            "WHERE b.active = true " +
            "AND (:q IS NULL " +
            "OR LOWER(b.name) LIKE :q ESCAPE '!')")
    Page<Brand> search(@Param("q") String q, Pageable pageable);
}
