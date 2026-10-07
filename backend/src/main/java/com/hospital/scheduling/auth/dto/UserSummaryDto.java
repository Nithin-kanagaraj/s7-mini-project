package com.hospital.scheduling.auth.dto;

import com.hospital.scheduling.common.security.UserPrincipal;
import com.hospital.scheduling.user.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto {
    private String id;
    private String username;
    private Role role;
    private String employeeId;
    private String email;
    private String fullName;
    private String departmentId;
    private String departmentName;

    public static UserSummaryDto fromPrincipal(UserPrincipal principal) {
        return UserSummaryDto.builder()
                .id(principal.getId())
                .username(principal.getUsername())
                .role(principal.getRole())
                .employeeId(principal.getEmployeeId())
                .email(principal.getEmail())
                .fullName(principal.getFullName())
                .departmentId(principal.getDepartmentId())
                .departmentName(principal.getDepartmentName())
                .build();
    }
}
