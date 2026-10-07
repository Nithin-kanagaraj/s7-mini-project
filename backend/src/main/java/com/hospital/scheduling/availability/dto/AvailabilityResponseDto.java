package com.hospital.scheduling.availability.dto;

import com.hospital.scheduling.availability.Availability;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityResponseDto {
    private String id;
    private String employeeId;
    private String employeeName;
    private LocalDate unavailableDate;
    private String shiftTemplateId;
    private String shiftTemplateName;
    private String reason;

    public static AvailabilityResponseDto fromEntity(Availability avail) {
        return AvailabilityResponseDto.builder()
                .id(avail.getId())
                .employeeId(avail.getEmployee() != null ? avail.getEmployee().getId() : null)
                .employeeName(avail.getEmployee() != null ? avail.getEmployee().getFirstName() + " " + avail.getEmployee().getLastName() : null)
                .unavailableDate(avail.getUnavailableDate())
                .shiftTemplateId(avail.getShiftTemplate() != null ? avail.getShiftTemplate().getId() : null)
                .shiftTemplateName(avail.getShiftTemplate() != null ? avail.getShiftTemplate().getName() : "All Shifts")
                .reason(avail.getReason())
                .build();
    }
}
