package br.com.fpo.sgc.domain.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Page<Employee> findByActiveTrue(Pageable pageable);

    Page<Employee> findByCompanyId(Long companyId, Pageable pageable);

    Page<Employee> findBySectorId(Long sectorId, Pageable pageable);

    boolean existsByCpf(String cpf);

    boolean existsByRegistrationNumber(String registrationNumber);

    // Listagem com busca e filtros opcionais (null = sem filtro). `q` já chega como padrão LIKE
    // montado por SearchTerm.toLikePattern. Sem `active`, lista ativos e inativos (comportamento original da listagem de funcionários).
    @Query("SELECT e FROM Employee e LEFT JOIN e.sector s " +
            "WHERE (:active IS NULL OR e.active = :active) " +
            "AND (:sectorId IS NULL OR s.id = :sectorId) " +
            "AND (:q IS NULL " +
            "OR LOWER(e.name) LIKE :q ESCAPE '!' " +
            "OR LOWER(e.cpf) LIKE :q ESCAPE '!' " +
            "OR LOWER(e.registrationNumber) LIKE :q ESCAPE '!')")
    Page<Employee> search(@Param("q") String q,
                          @Param("sectorId") Long sectorId,
                          @Param("active") Boolean active,
                          Pageable pageable);
}
