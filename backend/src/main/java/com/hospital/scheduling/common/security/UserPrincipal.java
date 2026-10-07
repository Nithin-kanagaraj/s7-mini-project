package com.hospital.scheduling.common.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hospital.scheduling.user.Role;
import com.hospital.scheduling.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Getter
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private final String id;
    private final String username;
    @JsonIgnore
    private final String password;
    private final Role role;
    private final String employeeId;
    private final String email;
    private final String fullName;
    private final String departmentId;
    private final String departmentName;
    private final Boolean isActive;
    private final Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal create(User user) {
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        String empId = null;
        String email = null;
        String fullName = null;
        String deptId = null;
        String deptName = null;

        if (user.getEmployee() != null) {
            empId = user.getEmployee().getId();
            email = user.getEmployee().getContactEmail();
            fullName = user.getEmployee().getFirstName() + " " + user.getEmployee().getLastName();
            if (user.getEmployee().getDepartment() != null) {
                deptId = user.getEmployee().getDepartment().getId();
                deptName = user.getEmployee().getDepartment().getName();
            }
        }

        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getRole(),
                empId,
                email,
                fullName,
                deptId,
                deptName,
                user.getIsActive(),
                authorities
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
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
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(isActive);
    }
}
