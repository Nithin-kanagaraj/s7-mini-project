-- Healthcare Shift Scheduling System Schema Initializer
-- MySQL 8 compatible DDL

CREATE TABLE departments (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE skills (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE employees (
    id CHAR(36) PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    employee_type VARCHAR(30) NOT NULL CHECK (employee_type IN ('DOCTOR','NURSE','LAB_TECH','PHARMACIST','SUPPORT')),
    department_id CHAR(36) NOT NULL,
    contact_email VARCHAR(150) NOT NULL UNIQUE,
    contact_phone VARCHAR(20) NULL,
    employment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (employment_status IN ('ACTIVE','ON_LEAVE','TERMINATED')),
    hire_date DATE NOT NULL,
    max_weekly_hours_override INT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_employees_department FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_employees_dept ON employees(department_id);
CREATE INDEX idx_employees_status ON employees(employment_status);

CREATE TABLE users (
    id CHAR(36) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN','HR','SCHEDULER','DEPT_HEAD','WORKER')),
    employee_id CHAR(36) NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at DATETIME NULL,
    CONSTRAINT fk_users_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_employee ON users(employee_id);

CREATE TABLE employee_skills (
    employee_id CHAR(36) NOT NULL,
    skill_id CHAR(36) NOT NULL,
    certified_date DATE NULL,
    expiry_date DATE NULL,
    PRIMARY KEY (employee_id, skill_id),
    CONSTRAINT fk_emp_skills_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT fk_emp_skills_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_emp_skills_skill ON employee_skills(skill_id);

CREATE TABLE shift_templates (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    duration_hours DECIMAL(4,2) NOT NULL CHECK (duration_hours > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE staffing_requirements (
    id CHAR(36) PRIMARY KEY,
    department_id CHAR(36) NOT NULL,
    shift_template_id CHAR(36) NOT NULL,
    shift_date DATE NOT NULL,
    employee_type VARCHAR(30) NOT NULL,
    required_skill_id CHAR(36) NULL,
    required_count INT NOT NULL CHECK (required_count >= 0),
    CONSTRAINT fk_staff_req_dept FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_staff_req_shift FOREIGN KEY (shift_template_id) REFERENCES shift_templates(id),
    CONSTRAINT fk_staff_req_skill FOREIGN KEY (required_skill_id) REFERENCES skills(id),
    CONSTRAINT uk_staffing_req UNIQUE (department_id, shift_template_id, shift_date, employee_type, required_skill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_staff_req_date ON staffing_requirements(shift_date);

CREATE TABLE availability (
    id CHAR(36) PRIMARY KEY,
    employee_id CHAR(36) NOT NULL,
    unavailable_date DATE NOT NULL,
    shift_template_id CHAR(36) NULL,
    reason VARCHAR(255) NULL,
    CONSTRAINT fk_avail_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT fk_avail_shift FOREIGN KEY (shift_template_id) REFERENCES shift_templates(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_avail_emp_date ON availability(employee_id, unavailable_date);

CREATE TABLE leave_requests (
    id CHAR(36) PRIMARY KEY,
    employee_id CHAR(36) NOT NULL,
    leave_type VARCHAR(20) NOT NULL CHECK (leave_type IN ('ANNUAL','SICK','EMERGENCY')),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    reason VARCHAR(255) NULL,
    approved_by CHAR(36) NULL,
    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at DATETIME NULL,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT chk_leave_dates CHECK (end_date >= start_date),
    CONSTRAINT fk_leave_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_leave_approver FOREIGN KEY (approved_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_leave_emp_range ON leave_requests(employee_id, start_date, end_date);

CREATE TABLE compliance_rules (
    id CHAR(36) PRIMARY KEY,
    rule_key VARCHAR(50) NOT NULL UNIQUE,
    rule_value INT NOT NULL,
    description TEXT NULL,
    updated_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_compliance_updater FOREIGN KEY (updated_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE schedules (
    id CHAR(36) PRIMARY KEY,
    department_id CHAR(36) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED')),
    generated_by CHAR(36) NOT NULL,
    generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at DATETIME NULL,
    has_shortages BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT chk_sched_period CHECK (period_end >= period_start),
    CONSTRAINT fk_sched_dept FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_sched_user FOREIGN KEY (generated_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_sched_dept_period ON schedules(department_id, period_start, period_end);

CREATE TABLE schedule_assignments (
    id CHAR(36) PRIMARY KEY,
    schedule_id CHAR(36) NULL,
    employee_id CHAR(36) NULL,
    department_id CHAR(36) NOT NULL,
    shift_template_id CHAR(36) NOT NULL,
    assignment_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED' CHECK (status IN ('ASSIGNED','UNFILLED','SWAP_PENDING','CANCELLED','NEEDS_REASSIGNMENT','PUBLISHED')),
    is_overtime BOOLEAN NOT NULL DEFAULT false,
    last_modified_by CHAR(36) NULL,
    last_modified_at DATETIME NULL,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_assign_sched FOREIGN KEY (schedule_id) REFERENCES schedules(id) ON DELETE CASCADE,
    CONSTRAINT fk_assign_emp FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_assign_dept FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_assign_shift FOREIGN KEY (shift_template_id) REFERENCES shift_templates(id),
    CONSTRAINT fk_assign_modifier FOREIGN KEY (last_modified_by) REFERENCES users(id),
    CONSTRAINT uk_assign_emp_date_shift UNIQUE (employee_id, assignment_date, shift_template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_assign_emp_date ON schedule_assignments(employee_id, assignment_date);
CREATE INDEX idx_assign_sched ON schedule_assignments(schedule_id);

CREATE TABLE attendance_records (
    id CHAR(36) PRIMARY KEY,
    assignment_id CHAR(36) NOT NULL,
    clock_in DATETIME NULL,
    clock_out DATETIME NULL,
    entry_method VARCHAR(20) NOT NULL DEFAULT 'MANUAL' CHECK (entry_method IN ('MANUAL','IMPORTED')),
    recorded_by CHAR(36) NULL,
    CONSTRAINT fk_attendance_assign FOREIGN KEY (assignment_id) REFERENCES schedule_assignments(id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_recorder FOREIGN KEY (recorded_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_attendance_assign ON attendance_records(assignment_id);

CREATE TABLE notifications (
    id CHAR(36) PRIMARY KEY,
    recipient_user_id CHAR(36) NOT NULL,
    type VARCHAR(30) NOT NULL CHECK (type IN ('SCHEDULE_PUBLISHED','SHIFT_CHANGED','LEAVE_DECISION','CONFLICT','SHIFT_REMINDER','EMERGENCY')),
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_recipient FOREIGN KEY (recipient_user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notif_user_read ON notifications(recipient_user_id, is_read);

CREATE TABLE audit_logs (
    id CHAR(36) PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id CHAR(36) NOT NULL,
    action VARCHAR(20) NOT NULL CHECK (action IN ('CREATE','UPDATE','DELETE','APPROVE','OVERRIDE')),
    performed_by CHAR(36) NOT NULL,
    old_value JSON NULL,
    new_value JSON NULL,
    performed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (performed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_performed ON audit_logs(performed_at);

CREATE TABLE refresh_tokens (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date DATETIME NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_refresh_token ON refresh_tokens(token);

