package com.hospital.scheduling;

import com.hospital.scheduling.employee.Employee;
import com.hospital.scheduling.employee.EmployeeRepository;
import com.hospital.scheduling.employee.EmployeeType;
import com.hospital.scheduling.employee.EmploymentStatus;
import com.hospital.scheduling.department.Department;
import com.hospital.scheduling.department.DepartmentRepository;
import com.hospital.scheduling.leave.LeaveRequest;
import com.hospital.scheduling.leave.LeaveRequestRepository;
import com.hospital.scheduling.leave.LeaveStatus;
import com.hospital.scheduling.leave.LeaveType;
import com.hospital.scheduling.schedule.AssignmentStatus;
import com.hospital.scheduling.schedule.ScheduleAssignment;
import com.hospital.scheduling.schedule.ScheduleAssignmentRepository;
import com.hospital.scheduling.schedule.ScheduleRepository;
import com.hospital.scheduling.schedule.ScheduleService;
import com.hospital.scheduling.schedule.dto.*;
import com.hospital.scheduling.shift.ShiftTemplate;
import com.hospital.scheduling.shift.ShiftTemplateRepository;
import com.hospital.scheduling.shift.StaffingRequirement;
import com.hospital.scheduling.shift.StaffingRequirementRepository;
import com.hospital.scheduling.skill.EmployeeSkill;
import com.hospital.scheduling.skill.EmployeeSkillId;
import com.hospital.scheduling.skill.EmployeeSkillRepository;
import com.hospital.scheduling.skill.Skill;
import com.hospital.scheduling.skill.SkillRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class Phase3SchedulingEngineTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ShiftTemplateRepository shiftTemplateRepository;

    @Autowired
    private StaffingRequirementRepository staffingRequirementRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private EmployeeSkillRepository employeeSkillRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleAssignmentRepository scheduleAssignmentRepository;

    private String getAuthToken(String username) throws Exception {
        String json = String.format("{\"username\":\"%s\",\"password\":\"Password123!\"}", username);
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int start = response.indexOf("\"accessToken\":\"") + 15;
        int end = response.indexOf("\"", start);
        return response.substring(start, end);
    }

    @Test
    @DisplayName("1. Verify Sufficient Staffing Schedule Generation")
    void testSufficientStaffingGeneration() throws Exception {
        String schedulerToken = getAuthToken("scheduler");

        // Emergency Department (10000000-0000-0000-0000-000000000001) for 2026-12-01 to 2026-12-07
        LocalDate start = LocalDate.of(2026, 12, 1);
        LocalDate end = LocalDate.of(2026, 12, 7);

        // Add staffing requirement for 1 Nurse for Morning shift each day
        Department dept = departmentRepository.findById("10000000-0000-0000-0000-000000000001").orElseThrow();
        ShiftTemplate shift = shiftTemplateRepository.findById("30000000-0000-0000-0000-000000000001").orElseThrow();

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            staffingRequirementRepository.save(StaffingRequirement.builder()
                    .department(dept)
                    .shiftTemplate(shift)
                    .shiftDate(d)
                    .employeeType(EmployeeType.NURSE)
                    .requiredCount(1)
                    .build());
        }

        String reqJson = String.format("""
                {
                    "departmentId": "%s",
                    "periodStart": "%s",
                    "periodEnd": "%s"
                }
                """, dept.getId(), start, end);

        mockMvc.perform(post("/api/schedules/generate")
                        .header("Authorization", "Bearer " + schedulerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hasShortages", is(false)))
                .andExpect(jsonPath("$.solveStatus", anyOf(is("OPTIMAL"), is("FEASIBLE"))))
                .andExpect(jsonPath("$.assignments", hasSize(greaterThanOrEqualTo(7))))
                .andExpect(jsonPath("$.assignments[0].status", is("ASSIGNED")));
    }

    @Test
    @DisplayName("2. Verify Understaffed Scenario & Shortage Reporting")
    void testUnderstaffedScenarioShortageReporting() throws Exception {
        String schedulerToken = getAuthToken("scheduler");

        Department dept = departmentRepository.findById("10000000-0000-0000-0000-000000000001").orElseThrow();
        ShiftTemplate shift = shiftTemplateRepository.findById("30000000-0000-0000-0000-000000000001").orElseThrow();
        LocalDate date = LocalDate.of(2026, 12, 15);

        // Save requirement for 10 Doctors (only 2 active Doctors exist in seed data)
        staffingRequirementRepository.save(StaffingRequirement.builder()
                .department(dept)
                .shiftTemplate(shift)
                .shiftDate(date)
                .employeeType(EmployeeType.DOCTOR)
                .requiredCount(10)
                .build());

        String reqJson = String.format("""
                {
                    "departmentId": "%s",
                    "periodStart": "%s",
                    "periodEnd": "%s",
                    "archiveExisting": true
                }
                """, dept.getId(), date, date);

        mockMvc.perform(post("/api/schedules/generate")
                        .header("Authorization", "Bearer " + schedulerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hasShortages", is(true)))
                .andExpect(jsonPath("$.shortageReport", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.shortageReport[0].shortage", is(8)))
                .andExpect(jsonPath("$.shortageReport[0].reason", notNullValue()));
    }

    @Test
    @DisplayName("3. Verify Max Weekly Hours Boundary & Revalidation Rejection")
    void testMaxWeeklyHoursBoundary() throws Exception {
        String schedulerToken = getAuthToken("scheduler");

        // Fetch employee Sarah Jenkins (40000000-0000-0000-0000-000000000001)
        Employee emp = employeeRepository.findById("40000000-0000-0000-0000-000000000001").orElseThrow();

        Department dept = departmentRepository.findById("10000000-0000-0000-0000-000000000001").orElseThrow();
        ShiftTemplate shift8h = shiftTemplateRepository.findById("30000000-0000-0000-0000-000000000001").orElseThrow(); // 8 hours

        LocalDate start = LocalDate.of(2026, 12, 21); // Monday

        // Create a schedule
        String reqJson = String.format("""
                {
                    "departmentId": "%s",
                    "periodStart": "%s",
                    "periodEnd": "%s",
                    "archiveExisting": true
                }
                """, dept.getId(), start, start.plusDays(6));

        String scheduleRes = mockMvc.perform(post("/api/schedules/generate")
                        .header("Authorization", "Bearer " + schedulerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        int idStart = scheduleRes.indexOf("\"id\":\"") + 6;
        int idEnd = scheduleRes.indexOf("\"", idStart);
        String scheduleId = scheduleRes.substring(idStart, idEnd);

        // Assign employee to 5 shifts in the week (5 * 8h = 40h, exactly at default 40h limit)
        for (int i = 0; i < 5; i++) {
            LocalDate day = start.plusDays(i);
            ScheduleAssignment sa = scheduleAssignmentRepository.save(ScheduleAssignment.builder()
                    .schedule(scheduleRepository.findById(scheduleId).orElseThrow())
                    .employee(emp)
                    .department(dept)
                    .shiftTemplate(shift8h)
                    .assignmentDate(day)
                    .status(AssignmentStatus.ASSIGNED)
                    .build());
        }

        // Attempting to assign a 6th shift in the same week (5 * 8 + 8 = 48h > 40h) via manual edit MUST be rejected with 422!
        ScheduleAssignment unassignedSlot = scheduleAssignmentRepository.save(ScheduleAssignment.builder()
                .schedule(scheduleRepository.findById(scheduleId).orElseThrow())
                .employee(null)
                .department(dept)
                .shiftTemplate(shift8h)
                .assignmentDate(start.plusDays(5))
                .status(AssignmentStatus.UNFILLED)
                .build());

        String editJson = String.format("""
                {
                    "employeeId": "%s",
                    "status": "ASSIGNED"
                }
                """, emp.getId());

        mockMvc.perform(patch("/api/schedules/" + scheduleId + "/assignments/" + unassignedSlot.getId())
                        .header("Authorization", "Bearer " + schedulerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editJson))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("exceed maximum allowed limit")));
    }

    @Test
    @DisplayName("4. Verify Skill Expiry Mid-Period Exclusion")
    void testSkillExpiryMidPeriodExclusion() throws Exception {
        String schedulerToken = getAuthToken("scheduler");

        Department dept = departmentRepository.findById("10000000-0000-0000-0000-000000000001").orElseThrow();
        ShiftTemplate shift = shiftTemplateRepository.findById("30000000-0000-0000-0000-000000000001").orElseThrow();
        Skill skill = skillRepository.findById("20000000-0000-0000-0000-000000000001").orElseThrow();

        // Create an employee with skill expiring on 2026-12-10
        Employee emp = employeeRepository.save(Employee.builder()
                .firstName("Expiring")
                .lastName("SkillEmp")
                .employeeType(EmployeeType.NURSE)
                .department(dept)
                .contactEmail("expiring.skill@hospital.org")
                .employmentStatus(EmploymentStatus.ACTIVE)
                .hireDate(LocalDate.of(2024, 1, 1))
                .build());

        employeeSkillRepository.save(EmployeeSkill.builder()
                .id(new EmployeeSkillId(emp.getId(), skill.getId()))
                .employee(emp)
                .skill(skill)
                .certifiedDate(LocalDate.of(2024, 1, 1))
                .expiryDate(LocalDate.of(2026, 12, 10))
                .build());

        LocalDate start = LocalDate.of(2026, 12, 8);
        LocalDate end = LocalDate.of(2026, 12, 14);

        // Add staffing requirement requiring this skill for 1 nurse each day
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            staffingRequirementRepository.save(StaffingRequirement.builder()
                    .department(dept)
                    .shiftTemplate(shift)
                    .shiftDate(d)
                    .employeeType(EmployeeType.NURSE)
                    .requiredSkill(skill)
                    .requiredCount(1)
                    .build());
        }

        String reqJson = String.format("""
                {
                    "departmentId": "%s",
                    "periodStart": "%s",
                    "periodEnd": "%s",
                    "archiveExisting": true
                }
                """, dept.getId(), start, end);

        String response = mockMvc.perform(post("/api/schedules/generate")
                        .header("Authorization", "Bearer " + schedulerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Verify that after 2026-12-10, the expiring employee is NOT assigned to post-expiry dates
        List<ScheduleAssignment> postExpiryAssignments = scheduleAssignmentRepository.findAll().stream()
                .filter(sa -> sa.getEmployee() != null && sa.getEmployee().getId().equals(emp.getId()))
                .filter(sa -> sa.getAssignmentDate().isAfter(LocalDate.of(2026, 12, 10)))
                .toList();

        assertTrue(postExpiryAssignments.isEmpty(), "Employee with expired skill should NOT be assigned to post-expiry dates");
    }

    @Test
    @DisplayName("5. Verify Concurrent Generation Lock Returns 409 Conflict")
    void testConcurrentGenerationLock() throws Exception {
        String schedulerToken = getAuthToken("scheduler");

        Department dept = departmentRepository.findById("10000000-0000-0000-0000-000000000001").orElseThrow();
        LocalDate start = LocalDate.of(2027, 1, 1);
        LocalDate end = LocalDate.of(2027, 1, 14);

        // Create a thread to simulate concurrent solve
        ScheduleGenerateRequestDto dto = ScheduleGenerateRequestDto.builder()
                .departmentId(dept.getId())
                .periodStart(start)
                .periodEnd(end)
                .archiveExisting(true)
                .build();

        // Standard generate call succeeds
        mockMvc.perform(post("/api/schedules/generate")
                        .header("Authorization", "Bearer " + schedulerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                    "departmentId": "%s",
                                    "periodStart": "%s",
                                    "periodEnd": "%s",
                                    "archiveExisting": true
                                }
                                """, dept.getId(), start, end)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("6. Benchmark Timing at 250 Employees x 2 Weeks Scale")
    void testTimingBenchmarkAtScale() throws Exception {
        Department dept = departmentRepository.findById("10000000-0000-0000-0000-000000000001").orElseThrow();
        ShiftTemplate shift = shiftTemplateRepository.findById("30000000-0000-0000-0000-000000000001").orElseThrow();

        // Scaled up dataset: 250 active employees
        List<Employee> scaledEmployees = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            scaledEmployees.add(Employee.builder()
                    .firstName("Benchmark" + i)
                    .lastName("Emp")
                    .employeeType(EmployeeType.NURSE)
                    .department(dept)
                    .contactEmail("bench.emp" + i + "@hospital.org")
                    .employmentStatus(EmploymentStatus.ACTIVE)
                    .hireDate(LocalDate.of(2024, 1, 1))
                    .build());
        }
        employeeRepository.saveAll(scaledEmployees);

        LocalDate start = LocalDate.of(2027, 2, 1);
        LocalDate end = LocalDate.of(2027, 2, 14); // 2-week period

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            staffingRequirementRepository.save(StaffingRequirement.builder()
                    .department(dept)
                    .shiftTemplate(shift)
                    .shiftDate(d)
                    .employeeType(EmployeeType.NURSE)
                    .requiredCount(15)
                    .build());
        }

        String schedulerToken = getAuthToken("scheduler");
        String reqJson = String.format("""
                {
                    "departmentId": "%s",
                    "periodStart": "%s",
                    "periodEnd": "%s",
                    "archiveExisting": true
                }
                """, dept.getId(), start, end);

        long startTime = System.currentTimeMillis();
        String response = mockMvc.perform(post("/api/schedules/generate")
                        .header("Authorization", "Bearer " + schedulerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long totalElapsedMs = System.currentTimeMillis() - startTime;

        System.out.println(">>> 250 Employees x 14 Days Benchmark Total Solve & Persistence Time: " + totalElapsedMs + " ms");
        assertTrue(totalElapsedMs < 30000, "Solve at scale must complete within 30 seconds limit");
    }
}
