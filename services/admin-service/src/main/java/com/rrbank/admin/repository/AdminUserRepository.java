package com.rrbank.admin.repository;

import com.rrbank.admin.entity.AdminUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {

    Optional<AdminUser> findByUsername(String username);

    Optional<AdminUser> findByEmail(String email);

    Optional<AdminUser> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT a FROM AdminUser a WHERE " +
           "(:search IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.lastName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:role IS NULL OR a.role = :role) " +
           "AND (:status IS NULL OR a.status = :status)")
    Page<AdminUser> findAllWithFilters(
            @Param("search") String search,
            @Param("role") AdminUser.AdminRole role,
            @Param("status") AdminUser.AdminStatus status,
            Pageable pageable
    );

    long countByStatus(AdminUser.AdminStatus status);

    long countByRole(AdminUser.AdminRole role);
}
