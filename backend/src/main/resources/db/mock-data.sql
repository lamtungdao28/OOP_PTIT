-- MySQL 8.4. Run against the schema created by the application.
-- Uses generated IDs and inserts missing fixtures without updating existing rows.
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO subjects (created_at, updated_at, subject_code, subject_name, credits, status)
SELECT CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), seed.code, seed.name, seed.credits, seed.status
FROM (
    SELECT 'MOCK-INT1001' AS code, 'Mock - Lap trinh can ban' AS name, 3 AS credits, 'ACTIVE' AS status
    UNION ALL SELECT 'MOCK-INT1002', 'Mock - Lap trinh huong doi tuong', 3, 'ACTIVE'
    UNION ALL SELECT 'MOCK-INT1003', 'Mock - Cau truc du lieu va giai thuat', 3, 'ACTIVE'
    UNION ALL SELECT 'MOCK-INT1004', 'Mock - Co so du lieu', 3, 'ACTIVE'
    UNION ALL SELECT 'MOCK-INT1005', 'Mock - Mang may tinh', 3, 'ACTIVE'
    UNION ALL SELECT 'MOCK-INT1006', 'Mock - He dieu hanh', 3, 'ACTIVE'
    UNION ALL SELECT 'MOCK-INT1007', 'Mock - Lap trinh web', 4, 'ACTIVE'
    UNION ALL SELECT 'MOCK-INT1008', 'Mock - Cong nghe phan mem', 3, 'ACTIVE'
    UNION ALL SELECT 'MOCK-INT1009', 'Mock - Tri tue nhan tao', 3, 'ACTIVE'
    UNION ALL SELECT 'MOCK-INT1010', 'Mock - Do an chuyen nganh', 2, 'INACTIVE'
) AS seed
WHERE NOT EXISTS (
    SELECT 1 FROM subjects existing
    WHERE existing.subject_code = seed.code OR existing.subject_name = seed.name
);

INSERT INTO semesters (created_at, updated_at, name)
SELECT CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), seed.name
FROM (
    SELECT 'Mock - Hoc ky 1 2025-2026' AS name
    UNION ALL SELECT 'Mock - Hoc ky 2 2025-2026'
    UNION ALL SELECT 'Mock - Hoc ky 1 2026-2027'
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM semesters existing WHERE existing.name = seed.name);

-- User.role currently uses ordinal persistence: STUDENT=0, LECTURER=1, ADMIN=2.
-- Fixture accounts are inactive and have non-login password markers.
INSERT INTO users (created_at, updated_at, username, email, password, role, status)
SELECT CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), seed.username,
       CONCAT(seed.username, '@example.test'),
       CONCAT('!mock-disabled-', SHA2(RANDOM_BYTES(32), 256)), 1, 'INACTIVE'
FROM (
    SELECT 'mock.lecturer.01' AS username
    UNION ALL SELECT 'mock.lecturer.02'
    UNION ALL SELECT 'mock.lecturer.03'
    UNION ALL SELECT 'mock.lecturer.04'
) AS seed
WHERE NOT EXISTS (
    SELECT 1 FROM users existing
    WHERE existing.username = seed.username OR existing.email = CONCAT(seed.username, '@example.test')
);

INSERT INTO lecturers (created_at, updated_at, lecturer_code, full_name, department, phone, usersid)
SELECT CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), seed.code, seed.name, seed.department, seed.phone, account.id
FROM (
    SELECT 'MOCK-GV001' AS code, 'Mock - Nguyen Van An' AS name, 'Cong nghe thong tin' AS department,
           '0000000001' AS phone, 'mock.lecturer.01' AS username
    UNION ALL SELECT 'MOCK-GV002', 'Mock - Tran Thi Binh', 'Khoa hoc may tinh', '0000000002', 'mock.lecturer.02'
    UNION ALL SELECT 'MOCK-GV003', 'Mock - Le Minh Chau', 'He thong thong tin', '0000000003', 'mock.lecturer.03'
    UNION ALL SELECT 'MOCK-GV004', 'Mock - Pham Quang Dung', 'Cong nghe phan mem', '0000000004', 'mock.lecturer.04'
) AS seed
JOIN users account ON account.username = seed.username AND account.email = CONCAT(seed.username, '@example.test')
WHERE NOT EXISTS (
    SELECT 1 FROM lecturers existing
    WHERE existing.lecturer_code = seed.code OR existing.usersid = account.id
);

-- Eight subjects in each of three semesters: 24 classes with varied capacities.
INSERT INTO course_classes
    (created_at, updated_at, class_code, max_students, status, subjectsid, semestersid, lecturersid)
SELECT CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), CONCAT(subject.subject_code, '-', period.code, '-01'),
       seed.max_students, period.status, subject.id, semester.id, lecturer.id
FROM (
    SELECT 'Mock - Hoc ky 1 2025-2026' AS name, '2526-HK1' AS code, 'INACTIVE' AS status
    UNION ALL SELECT 'Mock - Hoc ky 2 2025-2026', '2526-HK2', 'INACTIVE'
    UNION ALL SELECT 'Mock - Hoc ky 1 2026-2027', '2627-HK1', 'ACTIVE'
) AS period
CROSS JOIN (
    SELECT 'MOCK-INT1001' AS subject_code, 'MOCK-GV001' AS lecturer_code, 40 AS max_students
    UNION ALL SELECT 'MOCK-INT1002', 'MOCK-GV001', 45
    UNION ALL SELECT 'MOCK-INT1003', 'MOCK-GV002', 50
    UNION ALL SELECT 'MOCK-INT1004', 'MOCK-GV003', 60
    UNION ALL SELECT 'MOCK-INT1005', 'MOCK-GV002', 35
    UNION ALL SELECT 'MOCK-INT1006', 'MOCK-GV003', 40
    UNION ALL SELECT 'MOCK-INT1007', 'MOCK-GV004', 30
    UNION ALL SELECT 'MOCK-INT1008', 'MOCK-GV004', 50
) AS seed
JOIN subjects subject ON subject.subject_code = seed.subject_code
JOIN semesters semester ON semester.name = period.name
JOIN lecturers lecturer ON lecturer.lecturer_code = seed.lecturer_code
WHERE NOT EXISTS (
    SELECT 1 FROM course_classes existing
    WHERE existing.class_code = CONCAT(subject.subject_code, '-', period.code, '-01')
);

INSERT INTO semester_has_subject (created_at, updated_at, semestersid, subjectsid)
SELECT CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), seed.semestersid, seed.subjectsid
FROM (
    SELECT DISTINCT course.semestersid, course.subjectsid
    FROM course_classes course
    JOIN subjects subject ON subject.id = course.subjectsid
    JOIN semesters semester ON semester.id = course.semestersid
    WHERE course.class_code LIKE 'MOCK-%'
      AND subject.subject_code LIKE 'MOCK-%'
      AND semester.name LIKE 'Mock - %'
) AS seed
WHERE NOT EXISTS (
    SELECT 1 FROM semester_has_subject existing
    WHERE existing.semestersid = seed.semestersid AND existing.subjectsid = seed.subjectsid
);

COMMIT;

SELECT 'subjects' AS entity, COUNT(*) AS mock_count FROM subjects WHERE subject_code LIKE 'MOCK-%'
UNION ALL SELECT 'semesters', COUNT(*) FROM semesters WHERE name LIKE 'Mock - %'
UNION ALL SELECT 'course_classes', COUNT(*) FROM course_classes WHERE class_code LIKE 'MOCK-%'
UNION ALL SELECT 'lecturers', COUNT(*) FROM lecturers WHERE lecturer_code LIKE 'MOCK-%'
UNION ALL SELECT 'users', COUNT(*) FROM users WHERE username LIKE 'mock.lecturer.%';
