package com.rrbank.admin.controller;

import com.rrbank.admin.dto.AdminAuthDTOs.*;
import com.rrbank.admin.dto.common.ApiResponse;
import com.rrbank.admin.entity.Permission;
import com.rrbank.admin.entity.Role;
import com.rrbank.admin.repository.RoleRepository;
import com.rrbank.admin.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
@Slf4j
public class RoleController {

    private final RoleRepository roleRepository;

    @GetMapping
    @RequirePermission(anyOf = {Permission.RBAC_MANAGE, Permission.ADMIN_USER_READ})
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        List<RoleResponse> roles = roleRepository.findAll().stream()
                .map(this::toRoleResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    @GetMapping("/permissions")
    @RequirePermission(anyOf = {Permission.RBAC_MANAGE, Permission.ADMIN_USER_READ})
    public ResponseEntity<ApiResponse<List<String>>> getAllPermissions() {
        List<String> permissions = Arrays.stream(Permission.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    @GetMapping("/{roleName}")
    @RequirePermission(anyOf = {Permission.RBAC_MANAGE, Permission.ADMIN_USER_READ})
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleByName(@PathVariable String roleName) {
        Role role = roleRepository.findByName(roleName.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        return ResponseEntity.ok(ApiResponse.success(toRoleResponse(role)));
    }

    @GetMapping("/{roleName}/permissions")
    @RequirePermission(anyOf = {Permission.RBAC_MANAGE, Permission.ADMIN_USER_READ})
    public ResponseEntity<ApiResponse<Set<String>>> getRolePermissions(@PathVariable String roleName) {
        Role role = roleRepository.findByName(roleName.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        Set<String> permissions = role.getPermissions().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    private RoleResponse toRoleResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .isSystemRole(role.getIsSystemRole())
                .permissions(role.getPermissions().stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet()))
                .build();
    }
}
