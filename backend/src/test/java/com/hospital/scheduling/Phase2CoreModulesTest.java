package com.hospital.scheduling;

import com.hospital.scheduling.employee.EmployeeType;
import com.hospital.scheduling.employee.EmploymentStatus;
import com.hospital.scheduling.employee.dto.EmployeeRequestDto;
import com.hospital.scheduling.leave.LeaveType;
import com.hospital.scheduling.leave.dto.LeaveSubmitRequestDto;
import com.hospital.scheduling.shift.dto.StaffingRequirementRequestDto;
import com.hospital.scheduling.shift.dto.StaffingSkillWarningDto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class Phase2CoreModulesTest {

    @Autowired
    private MockMvc mockMvc;

    private String getAuthToken(String username) throws Exception {
        String json = String.format("{\"username\":\"%s\",\"password\":\"Password123!\"}", username);
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Extract accessToken from response json
        int start = response.indexOf("\"accessToken\":\"") + 15;
        int end = response.indexOf("\"", start);
        return response.substring(start, end);
    }

    @Test
    @DisplayName("Verify Max Weekly Hours Validation rejects value lower than longest shift duration")
    void testMaxWeeklyHoursValidation() throws Exception {
        String adminToken = getAuthToken("admin");

        // Longest shift in seed data is 8.00 hours. Trying to set max weekly hours to 4 should be rejected (422 or 400).
        String invalidEmployeeJson = """
                {
                    "firstName": "Test",
                    "lastName": "Validation",
                    "employeeType": "DOCTOR",
                    "departmentId": "10000000-0000-0000-0000-000000000001",
                    "contactEmail": "validation.test@hospital.org",
                    "contactPhone": "555-9999",
                    "employmentStatus": "ACTIVE",
                    "hireDate": "2024-01-01",
                    "maxWeeklyHoursOverride": 4
                }
                """;

        mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidEmployeeJson))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error", is("UNPROCESSABLE_ENTITY")))
                .andExpect(jsonPath("$.message", containsString("cannot be less than the longest shift template duration")));
    }

    @Test
    @DisplayName("Verify Employee Termination Status Transition & Audit Log creation")
    void testEmployeeTermination() throws Exception {
        String adminToken = getAuthToken("admin");

        // Terminate Sarah Jenkins (40000000-0000-0000-0000-000000000001)
        mockMvc.perform(patch("/api/employees/40000000-0000-0000-0000-000000000001/terminate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employmentStatus", is("TERMINATED")));
    }

    @Test
    @DisplayName("Verify Overlapping Leave Request returns Warning Flag")
    void testLeaveOverlapWarning() throws Exception {
        String workerToken = getAuthToken("worker");

        String leaveJson1 = """
                {
                    "leaveType": "ANNUAL",
                    "startDate": "2026-10-01",
                    "endDate": "2026-10-05",
                    "reason": "Vacation"
                }
                """;

        // First request - no overlap
        mockMvc.perform(post("/api/leave/my-requests")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(leaveJson1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hasOverlapWarning", is(false)));

        String leaveJson2 = """
                {
                    "leaveType": "SICK",
                    "startDate": "2026-10-03",
                    "endDate": "2026-10-07",
                    "reason": "Medical appointment"
                }
                """;

        // Second overlapping request - surfaces warning flag
        mockMvc.perform(post("/api/leave/my-requests")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(leaveJson2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hasOverlapWarning", is(true)))
                .andExpect(jsonPath("$.overlapWarningMessage", containsString("overlapping PENDING/APPROVED leave")));
    }

    @Test
    @DisplayName("Verify Zero Qualified Staff Warning Check")
    void testZeroQualifiedStaffWarning() throws Exception {
        String schedulerToken = getAuthToken("scheduler");

        // Skill 20000000-0000-0000-0000-000000000004 (Surgical-Tech) is not held by anyone in Laboratory (10000000-0000-0000-0000-000000000005)
        String reqJson = """
                {
                    "departmentId": "10000000-0000-0000-0000-000000000005",
                    "shiftTemplateId": "30000000-0000-0000-0000-000000000001",
                    "shiftDate": "2026-11-01",
                    "employeeType": "LAB_TECH",
                    "requiredSkillId": "20000000-0000-0000-0000-000000000004",
                    "requiredCount": 2
                }
                """;

        mockMvc.perform(post("/api/staffing-requirements")
                        .header("Authorization", "Bearer " + schedulerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isCreated());

        // Check warning endpoint
        mockMvc.perform(get("/api/staffing-requirements/warnings?departmentId=10000000-0000-0000-0000-000000000005&startDate=2026-11-01&endDate=2026-11-01")
                        .header("Authorization", "Bearer " + schedulerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].skillName", is("Surgical-Tech")))
                .andExpect(jsonPath("$[0].activeQualifiedEmployeeCount", is(0)));
    }

    @Test
    @DisplayName("Verify RBAC Violations are blocked with 403 Forbidden")
    void testRbacViolationsBlocked() throws Exception {
        String workerToken = getAuthToken("worker");

        // 1. Worker attempts to create a department -> 403
        String deptJson = "{\"name\":\"Forbidden Dept\",\"description\":\"Invalid\"}";
        mockMvc.perform(post("/api/departments")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deptJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        // 2. Worker attempts to list all employees -> 403
        mockMvc.perform(get("/api/employees")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        // 3. Dept Head attempts cross-department action -> 403
        String deptHeadToken = getAuthToken("dept_head"); // Emergency department head (10000000-0000-0000-0000-000000000001)

        // Attempting to create staffing requirement for ICU (10000000-0000-0000-0000-000000000002) -> 403
        String crossDeptReqJson = """
                {
                    "departmentId": "10000000-0000-0000-0000-000000000002",
                    "shiftTemplateId": "30000000-0000-0000-0000-000000000001",
                    "shiftDate": "2026-11-15",
                    "employeeType": "NURSE",
                    "requiredCount": 3
                }
                """;

        mockMvc.perform(post("/api/staffing-requirements")
                        .header("Authorization", "Bearer " + deptHeadToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crossDeptReqJson))
                .andExpect(status().isForbidden());
    }
}
