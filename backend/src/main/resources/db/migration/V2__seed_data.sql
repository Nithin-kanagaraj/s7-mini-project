-- Seed Data Migration for Healthcare Shift Scheduling System

-- 1. Departments (6 departments)
INSERT INTO departments (id, name, description, is_active) VALUES
('10000000-0000-0000-0000-000000000001', 'Emergency', 'Emergency Department & Trauma Unit', true),
('10000000-0000-0000-0000-000000000002', 'ICU', 'Intensive Care Unit', true),
('10000000-0000-0000-0000-000000000003', 'General Ward', 'Inpatient General Care Unit', true),
('10000000-0000-0000-0000-000000000004', 'Surgery', 'Operating Rooms & Surgical Unit', true),
('10000000-0000-0000-0000-000000000005', 'Laboratory', 'Pathology & Diagnostic Diagnostics', true),
('10000000-0000-0000-0000-000000000006', 'Pharmacy', 'Clinical Pharmacy Services', true);

-- 2. Skills (9 skills)
INSERT INTO skills (id, name, description) VALUES
('20000000-0000-0000-0000-000000000001', 'ICU-Certified', 'Specialized Intensive Care Nursing Certification'),
('20000000-0000-0000-0000-000000000002', 'ACLS', 'Advanced Cardiac Life Support'),
('20000000-0000-0000-0000-000000000003', 'Pediatric-Certified', 'Pediatric Specialization Certification'),
('20000000-0000-0000-0000-000000000004', 'Surgical-Tech', 'Certified Surgical Technologist'),
('20000000-0000-0000-0000-000000000005', 'Lab-Certified', 'Medical Laboratory Scientist License'),
('20000000-0000-0000-0000-000000000006', 'Pharmacy-Licensed', 'Registered Pharmacist License'),
('20000000-0000-0000-0000-000000000007', 'CPR-Basic', 'Basic Life Support / CPR'),
('20000000-0000-0000-0000-000000000008', 'Trauma-Advanced', 'Advanced Trauma Life Support'),
('20000000-0000-0000-0000-000000000009', 'Anesthesia-Care', 'Certified Registered Nurse Anesthetist / Assistant');

-- 3. Shift Templates (3 shift templates, 8 hours each)
INSERT INTO shift_templates (id, name, start_time, end_time, duration_hours) VALUES
('30000000-0000-0000-0000-000000000001', 'Morning', '07:00:00', '15:00:00', 8.00),
('30000000-0000-0000-0000-000000000002', 'Evening', '15:00:00', '23:00:00', 8.00),
('30000000-0000-0000-0000-000000000003', 'Night', '23:00:00', '07:00:00', 8.00);

-- 4. Employees (36 employees)
INSERT INTO employees (id, first_name, last_name, employee_type, department_id, contact_email, contact_phone, employment_status, hire_date) VALUES
-- Emergency Department
('40000000-0000-0000-0000-000000000001', 'Sarah', 'Jenkins', 'DOCTOR', '10000000-0000-0000-0000-000000000001', 'sjenkins@hospital.org', '555-0101', 'ACTIVE', '2018-03-15'),
('40000000-0000-0000-0000-000000000002', 'John', 'Doe', 'NURSE', '10000000-0000-0000-0000-000000000001', 'jdoe@hospital.org', '555-0102', 'ACTIVE', '2020-06-01'),
('40000000-0000-0000-0000-000000000003', 'Alex', 'Vance', 'SUPPORT', '10000000-0000-0000-0000-000000000001', 'avance@hospital.org', '555-0103', 'ACTIVE', '2019-01-10'),
('40000000-0000-0000-0000-000000000004', 'Helen', 'Rogers', 'SUPPORT', '10000000-0000-0000-0000-000000000001', 'hrogers@hospital.org', '555-0104', 'ACTIVE', '2021-04-12'),
('40000000-0000-0000-0000-000000000005', 'Sam', 'Miller', 'SUPPORT', '10000000-0000-0000-0000-000000000001', 'smiller@hospital.org', '555-0105', 'ACTIVE', '2017-09-01'),
('40000000-0000-0000-0000-000000000006', 'Rachel', 'Green', 'NURSE', '10000000-0000-0000-0000-000000000001', 'rgreen@hospital.org', '555-0106', 'ACTIVE', '2021-11-01'),
('40000000-0000-0000-0000-000000000007', 'Michael', 'Scott', 'DOCTOR', '10000000-0000-0000-0000-000000000001', 'mscott@hospital.org', '555-0107', 'ACTIVE', '2016-02-20'),
('40000000-0000-0000-0000-000000000008', 'Pam', 'Beesly', 'NURSE', '10000000-0000-0000-0000-000000000001', 'pbeesly@hospital.org', '555-0108', 'ACTIVE', '2022-01-15'),

-- ICU Department
('40000000-0000-0000-0000-000000000009', 'Jim', 'Halpert', 'DOCTOR', '10000000-0000-0000-0000-000000000002', 'jhalpert@hospital.org', '555-0109', 'ACTIVE', '2019-05-10'),
('40000000-0000-0000-0000-000000000010', 'Dwight', 'Schrute', 'NURSE', '10000000-0000-0000-0000-000000000002', 'dschrute@hospital.org', '555-0110', 'ACTIVE', '2018-08-14'),
('40000000-0000-0000-0000-000000000011', 'Angela', 'Martin', 'NURSE', '10000000-0000-0000-0000-000000000002', 'amartin@hospital.org', '555-0111', 'ACTIVE', '2020-03-01'),
('40000000-0000-0000-0000-000000000012', 'Kevin', 'Malone', 'NURSE', '10000000-0000-0000-0000-000000000002', 'kmalone@hospital.org', '555-0112', 'ACTIVE', '2021-07-19'),
('40000000-0000-0000-0000-000000000013', 'Oscar', 'Martinez', 'DOCTOR', '10000000-0000-0000-0000-000000000002', 'omartinez@hospital.org', '555-0113', 'ACTIVE', '2017-12-01'),
('40000000-0000-0000-0000-000000000014', 'Stanley', 'Hudson', 'NURSE', '10000000-0000-0000-0000-000000000002', 'shudson@hospital.org', '555-0114', 'ACTIVE', '2015-04-10'),

-- General Ward Department
('40000000-0000-0000-0000-000000000015', 'Phyllis', 'Vance', 'NURSE', '10000000-0000-0000-0000-000000000003', 'pvance@hospital.org', '555-0115', 'ACTIVE', '2019-10-05'),
('40000000-0000-0000-0000-000000000016', 'Meredith', 'Palmer', 'NURSE', '10000000-0000-0000-0000-000000000003', 'mpalmer@hospital.org', '555-0116', 'ACTIVE', '2020-02-15'),
('40000000-0000-0000-0000-000000000017', 'Creed', 'Bratton', 'SUPPORT', '10000000-0000-0000-0000-000000000003', 'cbratton@hospital.org', '555-0117', 'ACTIVE', '2014-06-01'),
('40000000-0000-0000-0000-000000000018', 'Kelly', 'Kapoor', 'NURSE', '10000000-0000-0000-0000-000000000003', 'kkapoor@hospital.org', '555-0118', 'ACTIVE', '2021-09-01'),
('40000000-0000-0000-0000-000000000019', 'Ryan', 'Howard', 'DOCTOR', '10000000-0000-0000-0000-000000000003', 'rhoward@hospital.org', '555-0119', 'ACTIVE', '2022-03-10'),
('40000000-0000-0000-0000-000000000020', 'Toby', 'Flenderson', 'NURSE', '10000000-0000-0000-0000-000000000003', 'tflenderson@hospital.org', '555-0120', 'ACTIVE', '2018-11-20'),

-- Surgery Department
('40000000-0000-0000-0000-000000000021', 'Darryl', 'Philbin', 'DOCTOR', '10000000-0000-0000-0000-000000000004', 'dphilbin@hospital.org', '555-0121', 'ACTIVE', '2016-07-01'),
('40000000-0000-0000-0000-000000000022', 'Erin', 'Hannon', 'NURSE', '10000000-0000-0000-0000-000000000004', 'ehannon@hospital.org', '555-0122', 'ACTIVE', '2020-08-12'),
('40000000-0000-0000-0000-000000000023', 'Andy', 'Bernard', 'DOCTOR', '10000000-0000-0000-0000-000000000004', 'abernard@hospital.org', '555-0123', 'ACTIVE', '2019-04-01'),
('40000000-0000-0000-0000-000000000024', 'Gabe', 'Lewis', 'NURSE', '10000000-0000-0000-0000-000000000004', 'glewis@hospital.org', '555-0124', 'ACTIVE', '2021-05-18'),
('40000000-0000-0000-0000-000000000025', 'Holly', 'Flax', 'NURSE', '10000000-0000-0000-0000-000000000004', 'hflax@hospital.org', '555-0125', 'ACTIVE', '2020-10-01'),
('40000000-0000-0000-0000-000000000026', 'Jan', 'Levinson', 'DOCTOR', '10000000-0000-0000-0000-000000000004', 'jlevinson@hospital.org', '555-0126', 'ACTIVE', '2015-09-15'),

-- Laboratory Department
('40000000-0000-0000-0000-000000000027', 'David', 'Wallace', 'LAB_TECH', '10000000-0000-0000-0000-000000000005', 'dwallace@hospital.org', '555-0127', 'ACTIVE', '2017-03-01'),
('40000000-0000-0000-0000-000000000028', 'Charles', 'Miner', 'LAB_TECH', '10000000-0000-0000-0000-000000000005', 'cminer@hospital.org', '555-0128', 'ACTIVE', '2019-08-01'),
('40000000-0000-0000-0000-000000000029', 'Karen', 'Filippelli', 'LAB_TECH', '10000000-0000-0000-0000-000000000005', 'kfilippelli@hospital.org', '555-0129', 'ACTIVE', '2020-04-15'),
('40000000-0000-0000-0000-000000000030', 'Roy', 'Anderson', 'LAB_TECH', '10000000-0000-0000-0000-000000000005', 'randerson@hospital.org', '555-0130', 'ACTIVE', '2021-01-20'),
('40000000-0000-0000-0000-000000000031', 'Bob', 'Vance', 'LAB_TECH', '10000000-0000-0000-0000-000000000005', 'bvance@hospital.org', '555-0131', 'ACTIVE', '2018-05-01'),

-- Pharmacy Department
('40000000-0000-0000-0000-000000000032', 'Nate', 'Nickerson', 'PHARMACIST', '10000000-0000-0000-0000-000000000006', 'nnickerson@hospital.org', '555-0132', 'ACTIVE', '2020-09-01'),
('40000000-0000-0000-0000-000000000033', 'Clark', 'Green', 'PHARMACIST', '10000000-0000-0000-0000-000000000006', 'cgreen@hospital.org', '555-0133', 'ACTIVE', '2021-06-01'),
('40000000-0000-0000-0000-000000000034', 'Pete', 'Miller', 'PHARMACIST', '10000000-0000-0000-0000-000000000006', 'pmiller@hospital.org', '555-0134', 'ACTIVE', '2022-02-01'),
('40000000-0000-0000-0000-000000000035', 'Nellie', 'Bertram', 'PHARMACIST', '10000000-0000-0000-0000-000000000006', 'nbertram@hospital.org', '555-0135', 'ACTIVE', '2019-11-01'),
('40000000-0000-0000-0000-000000000036', 'Robert', 'California', 'PHARMACIST', '10000000-0000-0000-0000-000000000006', 'rcalifornia@hospital.org', '555-0136', 'ACTIVE', '2017-01-15');

-- 5. Employee Skills (including an EXPIRED skill for testing)
INSERT INTO employee_skills (employee_id, skill_id, certified_date, expiry_date) VALUES
-- Sarah Jenkins
('40000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', '2022-01-01', '2027-01-01'), -- ACLS
('40000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000008', '2021-05-01', '2026-05-01'), -- Trauma-Advanced
-- John Doe (Worker user)
('40000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', '2023-02-01', '2028-02-01'), -- ACLS
('40000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000007', '2023-01-10', '2028-01-10'), -- CPR-Basic
-- Rachel Green -> EXPIRED SKILL for compliance testing!
('40000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000002', '2020-01-15', '2025-01-15'), -- ACLS Expired!
-- Jim Halpert (ICU Doctor)
('40000000-0000-0000-0000-000000000009', '20000000-0000-0000-0000-000000000001', '2021-03-01', '2027-03-01'), -- ICU-Certified
('40000000-0000-0000-0000-000000000009', '20000000-0000-0000-0000-000000000002', '2022-06-01', '2027-06-01'), -- ACLS
-- Dwight Schrute (ICU Nurse)
('40000000-0000-0000-0000-000000000010', '20000000-0000-0000-0000-000000000001', '2020-04-10', '2026-04-10'), -- ICU-Certified
-- Lab techs
('40000000-0000-0000-0000-000000000027', '20000000-0000-0000-0000-000000000005', '2017-03-01', '2027-03-01'), -- Lab-Certified
-- Pharmacists
('40000000-0000-0000-0000-000000000032', '20000000-0000-0000-0000-000000000006', '2020-09-01', '2030-09-01'); -- Pharmacy-Licensed

-- 6. Compliance Rules
INSERT INTO compliance_rules (id, rule_key, rule_value, description) VALUES
('50000000-0000-0000-0000-000000000001', 'MAX_WEEKLY_HOURS', 40, 'Maximum standard work hours allowed per employee per week'),
('50000000-0000-0000-0000-000000000002', 'MIN_REST_HOURS', 11, 'Minimum consecutive rest hours required between shift end and next shift start'),
('50000000-0000-0000-0000-000000000003', 'MAX_CONSECUTIVE_SHIFTS', 5, 'Maximum consecutive work days before mandatory rest day');

-- 7. Demo Users (5 users with bcrypt hash of 'Password123!')
-- BCrypt hash generated for Password123!: $2a$10$Bj7S8DT3fLjWgiJfgPbFtu8PA6Ouo5MEmlE3TFsy5d8aWuaKRbUv2
INSERT INTO users (id, username, password_hash, role, employee_id, is_active) VALUES
('60000000-0000-0000-0000-000000000001', 'admin', '$2a$10$Bj7S8DT3fLjWgiJfgPbFtu8PA6Ouo5MEmlE3TFsy5d8aWuaKRbUv2', 'ADMIN', '40000000-0000-0000-0000-000000000003', true),
('60000000-0000-0000-0000-000000000002', 'hr', '$2a$10$Bj7S8DT3fLjWgiJfgPbFtu8PA6Ouo5MEmlE3TFsy5d8aWuaKRbUv2', 'HR', '40000000-0000-0000-0000-000000000004', true),
('60000000-0000-0000-0000-000000000003', 'scheduler', '$2a$10$Bj7S8DT3fLjWgiJfgPbFtu8PA6Ouo5MEmlE3TFsy5d8aWuaKRbUv2', 'SCHEDULER', '40000000-0000-0000-0000-000000000005', true),
('60000000-0000-0000-0000-000000000004', 'dept_head', '$2a$10$Bj7S8DT3fLjWgiJfgPbFtu8PA6Ouo5MEmlE3TFsy5d8aWuaKRbUv2', 'DEPT_HEAD', '40000000-0000-0000-0000-000000000001', true),
('60000000-0000-0000-0000-000000000005', 'worker', '$2a$10$Bj7S8DT3fLjWgiJfgPbFtu8PA6Ouo5MEmlE3TFsy5d8aWuaKRbUv2', 'WORKER', '40000000-0000-0000-0000-000000000002', true);
