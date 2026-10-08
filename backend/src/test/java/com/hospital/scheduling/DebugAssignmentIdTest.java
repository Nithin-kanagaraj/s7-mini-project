package com.hospital.scheduling;

import com.hospital.scheduling.department.Department;
import com.hospital.scheduling.department.DepartmentRepository;
import com.hospital.scheduling.employee.Employee;
import com.hospital.scheduling.employee.EmployeeRepository;
import com.hospital.scheduling.schedule.ScheduleAssignment;
import com.hospital.scheduling.schedule.ScheduleAssignmentRepository;
import com.hospital.scheduling.shift.ShiftTemplate;
import com.hospital.scheduling.shift.ShiftTemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

@SpringBootTest
@ActiveProfiles("test")
public class DebugAssignmentIdTest {

    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private ShiftTemplateRepository shiftTemplateRepository;
    @Autowired private ScheduleAssignmentRepository assignmentRepository;

    @Test
    void debugAssignmentId() {
        Department department = new Department();
        department.setName("DBG-ASSIGN-" + System.nanoTime());
        department = departmentRepository.save(department);

        Employee employee = new Employee();
        employee.setEmployeeId("DBGEMP");
        employee.setFirstName("Debug");
        employee.setLastName("Employee");
        employee.setDepartment(department);
        employee.setContactEmail("dbg" + System.nanoTime() + "@test.local");
        employee.setEmploymentType("FULL_TIME");
        employee = employeeRepository.save(employee);

        ShiftTemplate st = new ShiftTemplate();
        st.setName("DBG-SHIFT");
        st.setStartTime(java.time.LocalTime.of(7,0));
        st.setEndTime(java.time.LocalTime.of(15,0));
        st.setDurationHours(new BigDecimal("8"));
        st = shiftTemplateRepository.save(st);

        ScheduleAssignment a = new ScheduleAssignment();
        a.setEmployee(employee);
        a.setShiftTemplate(st);
        a.setAssignmentDate(LocalDate.now().plusDays(5));
        a.setStatus("PUBLISHED");
        a.setIsOvertime(false);
        a = assignmentRepository.save(a);

        System.out.println("ID=" + a.getId());
        System.out.println("EMP=" + a.getEmployee().getId());
        System.out.println("DEPT_ID=" + (a.getDepartment() != null ? a.getDepartment().getId() : null));
        System.out.println("STATUS=" + a.getStatus());
    }
}
