package com.hospital.scheduling.department.dto;

import com.hospital.scheduling.department.Department;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentResponseDto {
    private String id;
    private String name;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private long employeeCount;

    public static DepartmentResponseDto fromEntity(Department dept, long employeeCount) {
        return DepartmentResponseDto.builder()
                .id(dept.getId())
                .name(dept.getName())
                .description(dept.getDescription())
                .isActive(dept.getIsActive())
                .createdAt(dept.getCreatedAt())
                .employeeCount(employeeCount)
                .build();
    }
}
