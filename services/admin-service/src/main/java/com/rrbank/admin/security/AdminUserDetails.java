package com.rrbank.admin.security;

import com.rrbank.admin.entity.AdminUser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AdminUserDetails implements UserDetails {

    private UUID id;
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private AdminUser.AdminRole role;
    private AdminUser.AdminStatus status;
    private boolean locked;

    public static AdminUserDetails from(AdminUser admin) {
        return new AdminUserDetails(
                admin.getId(),
                admin.getUsername(),
                admin.getEmail(),
                admin.getPasswordHash(),
                admin.getFirstName(),
                admin.getLastName(),
                admin.getRole(),
                admin.getStatus(),
                admin.isAccountLocked()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked && status != AdminUser.AdminStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == AdminUser.AdminStatus.ACTIVE;
    }

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
}
