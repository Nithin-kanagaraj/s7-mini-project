package com.hospital.scheduling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.scheduling.attendance.AttendanceService;
import com.hospital.scheduling.attendance.dto.AttendanceClockInRequestDto;
import com.hospital.scheduling.common.security.JwtTokenProvider;
import com.hospital.scheduling.compliance.ComplianceRule;
import com.hospital.scheduling.compliance.ComplianceRuleRepository;
import com.hospital.scheduling.department.Department;
import com.hospital.scheduling.department.DepartmentRepository;
import com.hospital.scheduling.employee.Employee;
import com.hospital.scheduling.employee.EmployeeRepository;
import com.hospital.scheduling.leave.LeaveRequest;
import com.hospital.scheduling.leave.LeaveRequestRepository;
import com.hospital.scheduling.leave.LeaveRequestService;
import com.hospital.scheduling.notification.NotificationRepository;
import com.hospital.scheduling.notification.NotificationService;
import com.hospital.scheduling.report.ReportService;
import com.hospital.scheduling.report.dto.CoverageReportDto;
import com.hospital.scheduling.report.dto.FairnessReportDto;
import com.hospital.scheduling.report.dto.OvertimeReportDto;
import com.hospital.scheduling.schedule.AssignmentStatus;
import com.hospital.scheduling.schedule.ScheduleAssignment;
import com.hospital.scheduling.schedule.ScheduleAssignmentRepository;
import com.hospital.scheduling.schedule.ScheduleService;
import com.hospital.scheduling.shift.ShiftTemplate;
import com.hospital.scheduling.shift.ShiftTemplateRepository;
import com.hospital.scheduling.user.User;
import com.hospital.scheduling.user.UserRepository;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 5 Comprehensive Test Suite.
 *
 * Covers:
 *   - AttendanceService: overtime recalculation (actual vs planned hours)
 *   - ReportService: coverage and fairness computations
 *   - NotificationService: event trigger verification via mock SimpMessagingTemplate
 *   - RBAC security: 401 unauthenticated, 403 wrong-role guards
 *   - Concurrent solver lock: second identical generate request returns 409
 *   - Leave approval after schedule published → NEEDS_REASSIGNMENT conflict
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Phase5ComprehensiveTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private PasswordEncoder passwordEncoder;

    // Repositories
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ShiftTemplateRepository shiftTemplateRepository;
    @Autowired private ScheduleAssignmentRepository assignmentRepository;
    @Autowired private ComplianceRuleRepository complianceRuleRepository;
    @Autowired private LeaveRequestRepository leaveRequestRepository;
    @Autowired private NotificationRepository notificationRepository;

    // Services under test
    @Autowired private AttendanceService attendanceService;
    @Autowired private ReportService reportService;
    @Autowired private LeaveRequestService leaveRequestService;

    // Mock WebSocket to avoid real broker in tests
    @MockBean private SimpMessagingTemplate messagingTemplate;

    private static String adminToken;
    private static String workerToken;
    private static String schedulerToken;
    private static String deptHeadToken;

    private static String testDeptId;
    private static String testEmpId;
    private static String testUserId;
    private static String testWorkerUserId;

    // ─────────────────────────────────────────────────────────────
    // SETUP
    // ─────────────────────────────────────────────────────────────

    @BeforeAll
    static void setUpTokens(@Autowired JwtTokenProvider jwtProvider,
                             @Autowired UserRepository userRepo,
                             @Autowired PasswordEncoder enc,
                             @Autowired DepartmentRepository deptRepo,
                             @Autowired EmployeeRepository empRepo,
                             @Autowired ShiftTemplateRepository stRepo,
                             @Autowired ComplianceRuleRepository crRepo) {

        // Compliance defaults (idempotent — seed may have already inserted them)
        if (crRepo.findByRuleKey("MAX_WEEKLY_HOURS").isEmpty()) {
            ComplianceRule r = new ComplianceRule();
            r.setRuleKey("MAX_WEEKLY_HOURS"); r.setRuleValue(new BigDecimal("40"));
            r.setDescription("Maximum weekly hours"); crRepo.save(r);
        }
        if (crRepo.findByRuleKey("MIN_REST_HOURS").isEmpty()) {
            ComplianceRule r = new ComplianceRule();
            r.setRuleKey("MIN_REST_HOURS"); r.setRuleValue(new BigDecimal("11"));
            r.setDescription("Minimum rest hours"); crRepo.save(r);
        }
        if (crRepo.findByRuleKey("MAX_CONSECUTIVE_SHIFTS").isEmpty()) {
            ComplianceRule r = new ComplianceRule();
            r.setRuleKey("MAX_CONSECUTIVE_SHIFTS"); r.setRuleValue(new BigDecimal("5"));
            r.setDescription("Max consecutive shifts"); crRepo.save(r);
        }

        // Department
        Department dept = new Department();
        dept.setName("Phase5-Test-Dept-" + UUID.randomUUID().toString().substring(0, 8));
        dept = deptRepo.save(dept);
        testDeptId = dept.getId();

        // Employee
        Employee emp = new Employee();
        emp.setEmployeeId("P5EMP001");
        emp.setFirstName("Test"); emp.setLastName("Worker");
        emp.setEmail("p5worker@test.local");
        emp.setDepartment(dept);
        emp.setEmploymentType("FULL_TIME");
        emp = empRepo.save(emp);
        testEmpId = emp.getId();

        // Admin user
        User admin = userRepo.findByUsername("admin5test").orElse(null);
        if (admin == null) {
            admin = new User();
            admin.setUsername("admin5test");
            admin.setPasswordHash(enc.encode("Password123!"));
            admin.setRole("ADMIN");
            admin = userRepo.save(admin);
        }
        testUserId = admin.getId();
        adminToken = jwtProvider.generateToken(admin.getUsername(), admin.getId(),
                admin.getRole(), null, null);

        // Worker user linked to employee
        User worker = userRepo.findByUsername("worker5test").orElse(null);
        if (worker == null) {
            worker = new User();
            worker.setUsername("worker5test");
            worker.setPasswordHash(enc.encode("Password123!"));
            worker.setRole("WORKER");
            worker.setEmployee(emp);
            worker = userRepo.save(worker);
        }
        testWorkerUserId = worker.getId();
        workerToken = jwtProvider.generateToken(worker.getUsername(), worker.getId(),
                worker.getRole(), emp.getId(), dept.getId());

        // Scheduler user
        User scheduler = userRepo.findByUsername("scheduler5test").orElse(null);
        if (scheduler == null) {
            scheduler = new User();
            scheduler.setUsername("scheduler5test");
            scheduler.setPasswordHash(enc.encode("Password123!"));
            scheduler.setRole("SCHEDULER");
            scheduler.setEmployee(emp); // dept scope via employee
            scheduler = userRepo.save(scheduler);
        }
        schedulerToken = jwtProvider.generateToken(scheduler.getUsername(), scheduler.getId(),
                scheduler.getRole(), null, dept.getId());

        // DeptHead user
        User deptHead = userRepo.findByUsername("depthead5test").orElse(null);
        if (deptHead == null) {
            deptHead = new User();
            deptHead.setUsername("depthead5test");
            deptHead.setPasswordHash(enc.encode("Password123!"));
            deptHead.setRole("DEPT_HEAD");
            deptHead.setEmployee(emp);
            deptHead = userRepo.save(deptHead);
        }
        deptHeadToken = jwtProvider.generateToken(deptHead.getUsername(), deptHead.getId(),
                deptHead.getRole(), null, dept.getId());
    }

    // ─────────────────────────────────────────────────────────────
    // SECTION 1 — ATTENDANCE SERVICE: OVERTIME CALCULATION
    // ─────────────────────────────────────────────────────────────

    /**
     * When actual hours exceed the configured MAX_WEEKLY_HOURS,
     * the assignment should be flagged is_overtime = true.
     */
    @Test
    @Order(10)
    void attendanceOvertime_actualHoursExceedWeeklyLimit_flagsIsOvertime() throws Exception {
        // Create a shift template (8h shift)
        ShiftTemplate st = shiftTemplateRepository.findAll().stream()
                .filter(s -> s.getDepartment() != null && s.getDepartment().getId().equals(testDeptId))
                .findFirst()
                .orElseGet(() -> {
                    ShiftTemplate t = new ShiftTemplate();
                    t.setName("P5-Day-Shift");
                    t.setStartTime(java.time.LocalTime.of(7, 0));
                    t.setEndTime(java.time.LocalTime.of(15, 0));
                    t.setDurationHours(new BigDecimal("8"));
                    t.setDepartment(departmentRepository.findById(testDeptId).orElseThrow());
                    return shiftTemplateRepository.save(t);
                });

        Employee emp = employeeRepository.findById(testEmpId).orElseThrow();
        LocalDate monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);

        // Create 5 assignments Mon–Fri (5 × 8h = 40h planned for the week)
        for (int i = 0; i < 5; i++) {
            ScheduleAssignment a = new ScheduleAssignment();
            a.setEmployee(emp);
            a.setShiftTemplate(st);
            a.setAssignmentDate(monday.plusDays(i));
            a.setStatus("ASSIGNED");
            a.setIsOvertime(false);
            assignmentRepository.save(a);
        }

        // Clock in/out for the Monday assignment with 10 actual hours (exceeds 8h planned,
        // and combined with rest of week's planned hours pushes over 40h cap)
        ScheduleAssignment mondayAssignment = assignmentRepository
                .findByEmployee_IdAndAssignmentDate(testEmpId, monday)
                .stream().findFirst().orElseThrow();

        AttendanceClockInRequestDto req = new AttendanceClockInRequestDto();
        req.setClockIn(monday.atTime(7, 0));
        req.setClockOut(monday.atTime(17, 0)); // 10 actual hours (2h overtime today)

        attendanceService.recordAttendance(mondayAssignment.getId(), req,
                userRepository.findByUsername("admin5test").orElseThrow().getId());

        // Refresh: if actual weekly hours > MAX_WEEKLY_HOURS (40) the flag should be set
        // In this test scenario, monday actual (10h) + tue-fri planned (4×8=32h) = 42h > 40
        ScheduleAssignment refreshed = assignmentRepository.findById(mondayAssignment.getId()).orElseThrow();
        assertThat(refreshed.getIsOvertime()).isTrue();
    }

    /**
     * When actual hours are within the weekly limit, is_overtime stays false.
     */
    @Test
    @Order(11)
    void attendanceOvertime_actualHoursWithinLimit_notFlagged() throws Exception {
        ShiftTemplate st = shiftTemplateRepository.findAll().stream()
                .filter(s -> s.getDepartment() != null && s.getDepartment().getId().equals(testDeptId))
                .findFirst()
                .orElseGet(() -> {
                    ShiftTemplate t = new ShiftTemplate();
                    t.setName("P5-Night-Shift");
                    t.setStartTime(java.time.LocalTime.of(23, 0));
                    t.setEndTime(java.time.LocalTime.of(7, 0));
                    t.setDurationHours(new BigDecimal("8"));
                    t.setDepartment(departmentRepository.findById(testDeptId).orElseThrow());
                    return shiftTemplateRepository.save(t);
                });

        Employee emp = employeeRepository.findById(testEmpId).orElseThrow();

        // Isolated date far in the future to avoid collision with other test assignments
        LocalDate isolatedDate = LocalDate.now().plusYears(2).with(java.time.DayOfWeek.TUESDAY);

        ScheduleAssignment a = new ScheduleAssignment();
        a.setEmployee(emp);
        a.setShiftTemplate(st);
        a.setAssignmentDate(isolatedDate);
        a.setStatus("ASSIGNED");
        a.setIsOvertime(false);
        a = assignmentRepository.save(a);

        // Clock 6h actual — well within weekly limit
        AttendanceClockInRequestDto req = new AttendanceClockInRequestDto();
        req.setClockIn(isolatedDate.atTime(23, 0));
        req.setClockOut(isolatedDate.plusDays(1).atTime(5, 0)); // 6 hours

        attendanceService.recordAttendance(a.getId(), req,
                userRepository.findByUsername("admin5test").orElseThrow().getId());

        ScheduleAssignment refreshed = assignmentRepository.findById(a.getId()).orElseThrow();
        assertThat(refreshed.getIsOvertime()).isFalse();
    }

    // ─────────────────────────────────────────────────────────────
    // SECTION 2 — REPORT SERVICE
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(20)
    void coverageReport_returnsDto_withValidStructure() {
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end   = LocalDate.now();

        CoverageReportDto report = reportService.getCoverageReport(testDeptId, start, end);

        assertThat(report).isNotNull();
        assertThat(report.getDepartmentId()).isEqualTo(testDeptId);
        assertThat(report.getStartDate()).isEqualTo(start);
        assertThat(report.getEndDate()).isEqualTo(end);
        // dailyBreakdown list must have exactly (end - start + 1) entries
        long expectedDays = start.datesUntil(end.plusDays(1)).count();
        assertThat(report.getDailyBreakdown()).hasSize((int) expectedDays);
    }

    @Test
    @Order(21)
    void overtimeReport_returnsDto_withValidStructure() {
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end   = LocalDate.now();

        OvertimeReportDto report = reportService.getOvertimeReport(testDeptId, start, end);

        assertThat(report).isNotNull();
        assertThat(report.getDepartmentId()).isEqualTo(testDeptId);
        // employeeOvertimeSummaries must be a non-null list (empty is fine if no overtime)
        assertThat(report.getEmployeeOvertimeSummaries()).isNotNull();
    }

    @Test
    @Order(22)
    void fairnessReport_varianceIsNonNegative() {
        FairnessReportDto report = reportService.getFairnessReport(testDeptId);

        assertThat(report).isNotNull();
        assertThat(report.getDepartmentId()).isEqualTo(testDeptId);
        // Variance is calculated as a non-negative value
        assertThat(report.getTotalShiftVariance()).isGreaterThanOrEqualTo(0.0);
        assertThat(report.getNightShiftVariance()).isGreaterThanOrEqualTo(0.0);
        assertThat(report.getWeekendShiftVariance()).isGreaterThanOrEqualTo(0.0);
    }

    // ─────────────────────────────────────────────────────────────
    // SECTION 3 — NOTIFICATION SERVICE: EVENT TRIGGERS
    // ─────────────────────────────────────────────────────────────

    /**
     * When a leave decision (APPROVED) is made and it overlaps a PUBLISHED assignment,
     * NotificationService must broadcast on /topic/schedules (conflict channel).
     */
    @Test
    @Order(30)
    void notificationService_leaveConflict_broadcastsToSchedulesTopic() {
        // This exercises the full LeaveRequestService.decideLeaveRequest() path which,
        // when an approved leave overlaps a PUBLISHED assignment, calls
        // notificationService.notifyLeaveConflict() → messagingTemplate.convertAndSend("/topic/schedules", ...)

        Employee emp = employeeRepository.findById(testEmpId).orElseThrow();

        // Create a PUBLISHED assignment for tomorrow
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        ShiftTemplate st = shiftTemplateRepository.findAll().stream()
                .filter(s -> s.getDepartment() != null && s.getDepartment().getId().equals(testDeptId))
                .findFirst().orElseGet(() -> {
                    ShiftTemplate t = new ShiftTemplate();
                    t.setName("P5-Conflict-Shift");
                    t.setStartTime(java.time.LocalTime.of(7, 0));
                    t.setEndTime(java.time.LocalTime.of(15, 0));
                    t.setDurationHours(new BigDecimal("8"));
                    t.setDepartment(departmentRepository.findById(testDeptId).orElseThrow());
                    return shiftTemplateRepository.save(t);
                });

        ScheduleAssignment published = new ScheduleAssignment();
        published.setEmployee(emp);
        published.setShiftTemplate(st);
        published.setAssignmentDate(tomorrow);
        published.setStatus("PUBLISHED");
        published.setIsOvertime(false);
        assignmentRepository.save(published);

        // Create a PENDING leave request covering tomorrow
        LeaveRequest lr = new LeaveRequest();
        lr.setEmployee(emp);
        lr.setStartDate(tomorrow);
        lr.setEndDate(tomorrow);
        lr.setLeaveType("SICK");
        lr.setReason("Phase 5 conflict test");
        lr.setStatus("PENDING");
        lr.setCreatedAt(LocalDateTime.now());
        lr = leaveRequestRepository.save(lr);

        // Approve it — this should trigger conflict notification
        User admin = userRepository.findByUsername("admin5test").orElseThrow();
        leaveRequestService.decideLeaveRequest(lr.getId(), "APPROVED", admin.getId());

        // Verify the assignment was marked NEEDS_REASSIGNMENT
        ScheduleAssignment updated = assignmentRepository.findById(published.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AssignmentStatus.NEEDS_REASSIGNMENT);

        // Verify WebSocket broadcast was triggered for conflict
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/schedules"), any(Object.class));
    }

    /**
     * NotificationService.notifySchedulePublished must write notification rows to DB
     * for all employees with assignments in the published schedule.
     */
    @Test
    @Order(31)
    void notificationService_schedulePublished_savesNotificationRecords() {
        // Verify via the repository that a notification exists for the worker user.
        // We trust the LeaveDecision test (above) and the ScheduleService.publishSchedule()
        // integration tested in Phase3SchedulingEngineTest. Here we do a direct service call.

        // Count existing notifications
        long before = notificationRepository.count();

        // Inject NotificationService and call directly
        com.hospital.scheduling.notification.NotificationService ns =
                applicationContext.getBean(com.hospital.scheduling.notification.NotificationService.class);

        // Create a dummy assignment in a schedule for the test employee
        Employee emp = employeeRepository.findById(testEmpId).orElseThrow();
        User worker = userRepository.findByUsername("worker5test").orElseThrow();
        LocalDate futureDate = LocalDate.now().plusDays(30);

        ShiftTemplate st = shiftTemplateRepository.findAll().stream()
                .filter(s -> s.getDepartment() != null && s.getDepartment().getId().equals(testDeptId))
                .findFirst().orElse(null);
        if (st == null) return; // skip if no shift template

        ScheduleAssignment a = new ScheduleAssignment();
        a.setEmployee(emp);
        a.setShiftTemplate(st);
        a.setAssignmentDate(futureDate);
        a.setStatus("ASSIGNED");
        a.setIsOvertime(false);
        a = assignmentRepository.save(a);

        // Simulate publish notification
        ns.notifySchedulePublished(List.of(a));

        long after = notificationRepository.count();
        // At least one new notification row should have been written
        assertThat(after).isGreaterThan(before);
    }

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    // ─────────────────────────────────────────────────────────────
    // SECTION 4 — RBAC SECURITY TESTS
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(40)
    void attendance_postEndpoint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/attendance/some-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(41)
    void attendance_postEndpoint_workerRole_returns403() throws Exception {
        mockMvc.perform(post("/api/attendance/some-id")
                .header("Authorization", "Bearer " + workerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        java.util.Map.of(
                                "clockIn",  "2025-01-01T08:00:00",
                                "clockOut", "2025-01-01T16:00:00"
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(42)
    void attendance_postEndpoint_schedulerRole_returns403() throws Exception {
        mockMvc.perform(post("/api/attendance/some-id")
                .header("Authorization", "Bearer " + schedulerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        java.util.Map.of(
                                "clockIn",  "2025-01-01T08:00:00",
                                "clockOut", "2025-01-01T16:00:00"
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(43)
    void auditLogs_getEndpoint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(44)
    void auditLogs_getEndpoint_workerRole_returns403() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(45)
    void auditLogs_getEndpoint_schedulerRole_returns403() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                .header("Authorization", "Bearer " + schedulerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(46)
    void auditLogs_getEndpoint_adminRole_returns200() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(47)
    void notifications_getEndpoint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(48)
    void notifications_emergencyEndpoint_workerRole_returns403() throws Exception {
        mockMvc.perform(post("/api/notifications/emergency")
                .header("Authorization", "Bearer " + workerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        java.util.Map.of("departmentId", testDeptId, "message", "Test"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(49)
    void reports_coverage_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/reports/coverage")
                .param("departmentId", testDeptId)
                .param("startDate", LocalDate.now().minusDays(7).toString())
                .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────────
    // SECTION 5 — CONCURRENT SOLVER LOCK: 409 ON SECOND REQUEST
    // ─────────────────────────────────────────────────────────────

    /**
     * Two simultaneous POST /api/schedules/generate for the same dept+period
     * must result in exactly one 202 (accepted) and one 409 (conflict).
     */
    @Test
    @Order(50)
    void schedulerGenerate_concurrent_secondRequestGets409() throws Exception {
        // Use a date range far in the future (won't collide with real data)
        String futureStart = LocalDate.now().plusYears(5).toString();
        String futureEnd   = LocalDate.now().plusYears(5).plusDays(6).toString();

        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "departmentId", testDeptId,
                "startDate",    futureStart,
                "endDate",      futureEnd
        ));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        AtomicInteger accepted = new AtomicInteger(0);
        AtomicInteger conflict = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        Callable<Integer> task = () -> {
            latch.await();
            try {
                var result = mockMvc.perform(post("/api/schedules/generate")
                        .header("Authorization", "Bearer " + schedulerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                        .andReturn();
                return result.getResponse().getStatus();
            } catch (Exception e) {
                return -1;
            }
        };

        Future<Integer> f1 = pool.submit(task);
        Future<Integer> f2 = pool.submit(task);
        latch.countDown(); // release both simultaneously

        int s1 = f1.get(30, TimeUnit.SECONDS);
        int s2 = f2.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        // Tally results. The generate endpoint returns 201 Created for the accepted request.
        for (int s : List.of(s1, s2)) {
            if (s == 201 || s == 202 || s == 200) accepted.incrementAndGet();
            else if (s == 409) conflict.incrementAndGet();
        }

        // Exactly one of each (order may vary)
        assertThat(accepted.get()).isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(1);
    }

    // ─────────────────────────────────────────────────────────────
    // SECTION 6 — LEAVE APPROVAL AFTER PUBLISH → NEEDS_REASSIGNMENT
    // ─────────────────────────────────────────────────────────────

    /**
     * Full integration path:
     * 1. Create a PUBLISHED assignment for an employee
     * 2. Approve a leave request that overlaps that assignment
     * 3. Assert the assignment status changes to NEEDS_REASSIGNMENT
     * 4. Assert a CONFLICT notification was persisted to DB for the dept
     */
    @Test
    @Order(60)
    void leaveApproval_afterPublish_setsNeedsReassignment_andCreatesConflictNotification() {
        Employee emp = employeeRepository.findById(testEmpId).orElseThrow();

        LocalDate leaveDate = LocalDate.now().plusMonths(3);

        ShiftTemplate st = shiftTemplateRepository.findAll().stream()
                .filter(s -> s.getDepartment() != null && s.getDepartment().getId().equals(testDeptId))
                .findFirst().orElseGet(() -> {
                    ShiftTemplate t = new ShiftTemplate();
                    t.setName("P5-Leave-Conflict-Shift");
                    t.setStartTime(java.time.LocalTime.of(7, 0));
                    t.setEndTime(java.time.LocalTime.of(15, 0));
                    t.setDurationHours(new BigDecimal("8"));
                    t.setDepartment(departmentRepository.findById(testDeptId).orElseThrow());
                    return shiftTemplateRepository.save(t);
                });

        // Step 1: PUBLISHED assignment on that day
        ScheduleAssignment pubAssignment = new ScheduleAssignment();
        pubAssignment.setEmployee(emp);
        pubAssignment.setShiftTemplate(st);
        pubAssignment.setAssignmentDate(leaveDate);
        pubAssignment.setStatus("PUBLISHED");
        pubAssignment.setIsOvertime(false);
        pubAssignment = assignmentRepository.save(pubAssignment);

        // Step 2: PENDING leave covering that day
        LeaveRequest lr = new LeaveRequest();
        lr.setEmployee(emp);
        lr.setStartDate(leaveDate);
        lr.setEndDate(leaveDate);
        lr.setLeaveType("ANNUAL");
        lr.setReason("Phase 5 integration: leave-after-publish");
        lr.setStatus("PENDING");
        lr.setCreatedAt(LocalDateTime.now());
        lr = leaveRequestRepository.save(lr);

        long notifsBefore = notificationRepository.count();

        // Step 3: Approve
        User admin = userRepository.findByUsername("admin5test").orElseThrow();
        leaveRequestService.decideLeaveRequest(lr.getId(), "APPROVED", admin.getId());

        // Step 4: Verify NEEDS_REASSIGNMENT
        ScheduleAssignment updated = assignmentRepository.findById(pubAssignment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AssignmentStatus.NEEDS_REASSIGNMENT);

        // Step 5: Verify at least one CONFLICT notification was persisted
        long notifsAfter = notificationRepository.count();
        assertThat(notifsAfter).isGreaterThan(notifsBefore);

        // Step 6: WebSocket broadcast was triggered
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/schedules"), any(Object.class));
    }

    // ─────────────────────────────────────────────────────────────
    // SECTION 7 — OPTIMISTIC LOCK / CONCURRENT EDIT → 409
    // ─────────────────────────────────────────────────────────────

    /**
     * Verifies that the manual-edit endpoint returns 409 when a stale `version`
     * is supplied (optimistic locking guard).
     */
    @Test
    @Order(70)
    void manualEdit_staleVersion_returns409() throws Exception {
        Employee emp = employeeRepository.findById(testEmpId).orElseThrow();
        ShiftTemplate st = shiftTemplateRepository.findAll().stream()
                .filter(s -> s.getDepartment() != null && s.getDepartment().getId().equals(testDeptId))
                .findFirst().orElseGet(() -> {
                    ShiftTemplate t = new ShiftTemplate();
                    t.setName("P5-Lock-Shift");
                    t.setStartTime(java.time.LocalTime.of(7, 0));
                    t.setEndTime(java.time.LocalTime.of(15, 0));
                    t.setDurationHours(new BigDecimal("8"));
                    t.setDepartment(departmentRepository.findById(testDeptId).orElseThrow());
                    return shiftTemplateRepository.save(t);
                });

        LocalDate lockDate = LocalDate.now().plusMonths(6);

        ScheduleAssignment a = new ScheduleAssignment();
        a.setEmployee(emp);
        a.setShiftTemplate(st);
        a.setAssignmentDate(lockDate);
        a.setStatus("ASSIGNED");
        a.setIsOvertime(false);
        a = assignmentRepository.save(a);
        final String assignId = a.getId();

        // Send a PATCH with version=9999 (wrong, should be 0 or current)
        mockMvc.perform(patch("/api/schedules/assignments/" + assignId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                        "employeeId",      testEmpId,
                        "version",         9999
                ))))
                .andExpect(status().isConflict()); // 409
    }
}
