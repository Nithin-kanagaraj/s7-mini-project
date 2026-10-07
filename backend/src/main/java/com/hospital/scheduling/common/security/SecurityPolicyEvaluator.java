package com.hospital.scheduling.common.security;

import com.hospital.scheduling.user.Role;
import org.springframework.stereotype.Component;

@Component("securityPolicy")
public class SecurityPolicyEvaluator {

    public boolean isRole(UserPrincipal currentUser, String roleName) {
        if (currentUser == null || currentUser.getRole() == null) {
            return false;
        }
        return currentUser.getRole().name().equalsIgnoreCase(roleName);
    }

    public boolean hasAnyRole(UserPrincipal currentUser, String... roleNames) {
        if (currentUser == null || currentUser.getRole() == null) {
            return false;
        }
        for (String r : roleNames) {
            if (currentUser.getRole().name().equalsIgnoreCase(r)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSelfOrAdmin(UserPrincipal currentUser, String targetUserId) {
        if (currentUser == null) return false;
        if (currentUser.getRole() == Role.ADMIN) return true;
        return currentUser.getId().equals(targetUserId);
    }

    public boolean canManageDepartment(UserPrincipal currentUser, String departmentId) {
        if (currentUser == null) return false;
        if (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.HR) return true;
        if (currentUser.getRole() == Role.DEPT_HEAD) {
            return departmentId != null && departmentId.equals(currentUser.getDepartmentId());
        }
        return false;
    }
}
