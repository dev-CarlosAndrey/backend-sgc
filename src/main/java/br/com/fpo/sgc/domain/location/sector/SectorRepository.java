package br.com.fpo.sgc.domain.location.sector;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Long> {
    Page<Sector> findByActiveTrue(Pageable pageable);
    Page<Sector> findByBuildingIdAndActiveTrue(Long buildingId, Pageable pageable);
    long countByBuildingId(Long buildingId);
    long countByBuildingIdAndActiveTrue(Long buildingId);
    // Usado pela abertura de OS via WhatsApp (RF15): o nome do setor vem em texto livre do usuário.
    Optional<Sector> findByNameIgnoreCaseAndActiveTrue(String name);

    // Listagem com busca e filtros opcionais (null = sem filtro). `q` já chega como padrão LIKE
    // montado por SearchTerm.toLikePattern.
    @Query("SELECT s FROM Sector s LEFT JOIN s.building b LEFT JOIN s.manager m " +
            "WHERE s.active = true " +
            "AND (:q IS NULL " +
            "OR LOWER(s.name) LIKE :q ESCAPE '!' " +
            "OR LOWER(b.name) LIKE :q ESCAPE '!' " +
            "OR LOWER(m.name) LIKE :q ESCAPE '!')")
    Page<Sector> search(@Param("q") String q, Pageable pageable);
}
