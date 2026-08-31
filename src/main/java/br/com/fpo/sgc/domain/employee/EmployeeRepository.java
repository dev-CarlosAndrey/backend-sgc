package br.com.fpo.sgc.domain.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Page<Employee> findByActiveTrue(Pageable pageable);

    Page<Employee> findByCompanyId(Long companyId, Pageable pageable);

    Page<Employee> findBySectorId(Long sectorId, Pageable pageable);

    boolean existsByCpf(String cpf);

    boolean existsByRegistrationNumber(String registrationNumber);
}