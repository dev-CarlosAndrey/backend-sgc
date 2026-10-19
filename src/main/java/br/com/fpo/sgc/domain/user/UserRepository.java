package br.com.fpo.sgc.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByNameAndActiveTrue(String name);
    Optional<User> findByPhone(String phone);
    boolean existsByEmail(String email);
    Page<User> findByActiveTrue(Pageable pageable);

    // Listagem com busca e filtros opcionais (null = sem filtro). `q` já chega como padrão LIKE
    // montado por SearchTerm.toLikePattern.
    @Query("SELECT u FROM User u " +
            "WHERE u.active = true " +
            "AND (:role IS NULL OR u.role = :role) " +
            "AND (:q IS NULL " +
            "OR LOWER(u.name) LIKE :q ESCAPE '!' " +
            "OR LOWER(u.email) LIKE :q ESCAPE '!')")
    Page<User> search(@Param("q") String q, @Param("role") Role role, Pageable pageable);
}
