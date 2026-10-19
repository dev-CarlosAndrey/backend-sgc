package br.com.fpo.sgc.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByActiveTrue(Pageable pageable);

    // Listagem com busca e filtros opcionais (null = sem filtro). `q` já chega como padrão LIKE
    // montado por SearchTerm.toLikePattern.
    @Query("SELECT p FROM Product p LEFT JOIN p.brand b LEFT JOIN p.material m " +
            "WHERE p.active = true " +
            "AND (:q IS NULL " +
            "OR LOWER(p.name) LIKE :q ESCAPE '!' " +
            "OR LOWER(b.name) LIKE :q ESCAPE '!' " +
            "OR LOWER(m.name) LIKE :q ESCAPE '!' " +
            "OR LOWER(m.category) LIKE :q ESCAPE '!')")
    Page<Product> search(@Param("q") String q, Pageable pageable);
}
