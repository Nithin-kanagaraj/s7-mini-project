package com.hospital.scheduling.shift.dto;

import com.hospital.scheduling.shift.ShiftTemplate;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftTemplateResponseDto {
    private String id;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal durationHours;
    private boolean isOvernight;

    public static ShiftTemplateResponseDto fromEntity(ShiftTemplate st) {
        boolean overnight = st.getEndTime() != null && st.getStartTime() != null && !st.getEndTime().isAfter(st.getStartTime());
        return ShiftTemplateResponseDto.builder()
                .id(st.getId())
                .name(st.getName())
                .startTime(st.getStartTime())
                .endTime(st.getEndTime())
                .durationHours(st.getDurationHours())
                .isOvernight(overnight)
                .build();
    }
}
