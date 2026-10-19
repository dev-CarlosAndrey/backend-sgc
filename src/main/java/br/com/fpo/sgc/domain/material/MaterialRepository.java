package br.com.fpo.sgc.domain.material;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {
    Page<Material> findByActiveTrue(Pageable pageable);

    // Listagem com busca e filtros opcionais (null = sem filtro). `q` já chega como padrão LIKE
    // montado por SearchTerm.toLikePattern.
    @Query("SELECT m FROM Material m " +
            "WHERE m.active = true " +
            "AND (:q IS NULL " +
            "OR LOWER(m.name) LIKE :q ESCAPE '!' " +
            "OR LOWER(m.category) LIKE :q ESCAPE '!')")
    Page<Material> search(@Param("q") String q, Pageable pageable);
}
