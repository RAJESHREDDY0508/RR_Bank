package com.rrbank.admin.repository;

import com.rrbank.admin.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    
    Optional<Role> findByName(String name);
    
    boolean existsByName(String name);
    
    List<Role> findByIsSystemRoleTrue();
    
    List<Role> findByIsSystemRoleFalse();
    
    @Query("SELECT r FROM Role r WHERE r.name IN :names")
    List<Role> findByNameIn(List<String> names);
}
