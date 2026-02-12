package com.rrbank.admin.security;

import com.rrbank.admin.entity.AdminUser;
import com.rrbank.admin.entity.Permission;
import com.rrbank.admin.exception.ForbiddenException;
import com.rrbank.admin.repository.AdminUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

/**
 * Interceptor that enforces permission requirements on endpoints
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionInterceptor implements HandlerInterceptor {

    private final AdminUserRepository adminUserRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        
        // Check method-level annotation first, then class-level
        RequirePermission methodAnnotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
        RequirePermission classAnnotation = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        
        RequirePermission annotation = methodAnnotation != null ? methodAnnotation : classAnnotation;
        
        if (annotation == null) {
            // No permission required
            return true;
        }

        // Get user ID from request attribute (set by JWT filter)
        String userIdStr = (String) request.getAttribute("adminUserId");
        if (userIdStr == null) {
            log.warn("No adminUserId found in request for protected endpoint: {}", request.getRequestURI());
            throw new ForbiddenException("Authentication required");
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            throw new ForbiddenException("Invalid user ID");
        }

        // Load user with roles and permissions
        AdminUser admin = adminUserRepository.findById(userId)
                .orElseThrow(() -> new ForbiddenException("User not found"));

        Set<Permission> userPermissions = admin.getAllPermissions();
        
        // Check if user is super admin (has all permissions)
        if (admin.hasRole("SUPER_ADMIN")) {
            return true;
        }

        // Check required permissions
        Permission[] requiredPermissions = annotation.value().length > 0 ? annotation.value() : annotation.anyOf();
        
        if (requiredPermissions.length == 0) {
            return true;
        }

        boolean hasPermission;
        if (annotation.requireAll() && annotation.value().length > 0) {
            // Must have ALL permissions
            hasPermission = Arrays.stream(requiredPermissions)
                    .allMatch(userPermissions::contains);
        } else {
            // Must have ANY permission
            hasPermission = Arrays.stream(requiredPermissions)
                    .anyMatch(userPermissions::contains);
        }

        if (!hasPermission) {
            log.warn("Permission denied for user {} on endpoint {}: required={}, has={}",
                    admin.getUsername(), request.getRequestURI(), 
                    Arrays.toString(requiredPermissions), userPermissions);
            throw new ForbiddenException("You don't have permission to access this resource");
        }

        // Store permissions in request for use by controllers
        request.setAttribute("userPermissions", userPermissions);
        request.setAttribute("adminUser", admin);

        return true;
    }
}
