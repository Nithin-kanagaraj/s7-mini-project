package com.hospital.scheduling.schedule;

import com.hospital.scheduling.common.security.UserPrincipal;
import com.hospital.scheduling.department.Department;
import com.hospital.scheduling.employee.Employee;
import com.hospital.scheduling.schedule.dto.ScheduleAssignmentResponseDto;
import com.hospital.scheduling.shift.ShiftTemplate;
import com.hospital.scheduling.user.Role;
import com.hospital.scheduling.user.User;
import com.hospital.scheduling.user.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublishedRosterServiceTest {

    @Test
    void getMyAssignments_usesEmployeeIdFromUserWhenTokenHasNone() {
        ScheduleAssignmentRepository assignmentRepository = mock(ScheduleAssignmentRepository.class);
        UserRepository userRepository = mock(UserRepository.class);

        ScheduleService service = new ScheduleService(
                null,
                assignmentRepository,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                userRepository,
                null,
                null,
                null,
                null,
                null
        );

        User user = User.builder()
                .id("user-1")
                .role(Role.WORKER)
                .employee(Employee.builder()
                        .id("emp-1")
                        .firstName("Sam")
                        .lastName("Miller")
                        .department(Department.builder().id("dept-1").name("ICU").build())
                        .build())
                .build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        Department dept = Department.builder().id("dept-1").name("ICU").build();
        ShiftTemplate shift = ShiftTemplate.builder()
                .id("shift-1")
                .name("Morning")
                .startTime(LocalTime.of(7, 0))
                .endTime(LocalTime.of(15, 0))
                .durationHours(new java.math.BigDecimal("8.00"))
                .build();

        ScheduleAssignment assignment = ScheduleAssignment.builder()
                .id("assignment-1")
                .employee(user.getEmployee())
                .department(dept)
                .shiftTemplate(shift)
                .assignmentDate(LocalDate.of(2026, 10, 12))
                .status(AssignmentStatus.PUBLISHED)
                .isOvertime(false)
                .build();

        when(assignmentRepository.findMyPublishedAssignments("emp-1")).thenReturn(List.of(assignment));

        UserPrincipal principal = new UserPrincipal(
                "user-1",
                "worker",
                "pw",
                Role.WORKER,
                null,
                null,
                null,
                null,
                null,
                true,
                Collections.emptyList()
        );

        List<ScheduleAssignmentResponseDto> result = service.getMyAssignments(principal);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmployeeId()).isEqualTo("emp-1");
        assertThat(result.get(0).getShiftTemplateName()).isEqualTo("Morning");
    }
}
