-- Manual-only mock data for MySQL 8.4 and the current JPA schema.
-- Creating this file does not import it. It is outside Spring's resource path.
-- Use only in a development database, with application writes stopped.
-- No USE, DDL, DELETE, UPDATE, REPLACE, TRUNCATE or disabled FK checks.
-- Stop on the first SQL error and ROLLBACK; do not use a continue-on-error mode.
-- Existing rows are never overwritten. Run one copy at a time.
-- Reserved namespace: DEMO-*, demo.*@example.test, session_no 940011..940054.
-- Table/column spelling follows Hibernate's current physical naming strategy.
-- In particular, class_seesionsid and attendnce_sessionsid are intentional.

SET NAMES utf8mb4;

-- Optional: supply @demo_password_hash in the same SQL session before import.
-- It must be a BCrypt hash of a password you choose, NOT a plaintext password.
-- Without a valid hash, ALL inserted accounts are INACTIVE and cannot log in.
-- No default password or reusable administrator credential is included.
SET @demo_has_login = REGEXP_LIKE(COALESCE(@demo_password_hash, ''),
    '^[$]2[aby][$](0[4-9]|[12][0-9]|3[01])[$][./A-Za-z0-9]{53}$', 'c');
SET @demo_now = CURRENT_TIMESTAMP(6);
SET @demo_today = DATE(@demo_now);
SET @demo_open_target = DATE_ADD(@demo_now, INTERVAL 2 HOUR);
SET @demo_open_deadline = TIMESTAMP(DATE(@demo_open_target), MAKETIME(HOUR(@demo_open_target), 30, 0));

START TRANSACTION;

-- 1. Accounts: 1 admin, 3 lecturers, 20 students.
-- User.role is ordinal: STUDENT=0, LECTURER=1, ADMIN=2.
-- Student 20 remains INACTIVE even when a valid password hash is supplied.
INSERT INTO users (created_at, updated_at, username, email, password, role, status)
SELECT @demo_now, @demo_now, seed.username, CONCAT(seed.username, '@example.test'),
       CASE WHEN @demo_has_login THEN @demo_password_hash
            ELSE CONCAT('!demo-disabled-', SHA2(RANDOM_BYTES(32), 256)) END,
       seed.role,
       CASE WHEN @demo_has_login AND seed.enabled = 1 THEN 'ACTIVE' ELSE 'INACTIVE' END
FROM (
    SELECT 'demo.admin' AS username, 2 AS role, 1 AS enabled
    UNION ALL SELECT 'demo.lecturer.01', 1, 1
    UNION ALL SELECT 'demo.lecturer.02', 1, 1
    UNION ALL SELECT 'demo.lecturer.03', 1, 1
    UNION ALL
    SELECT CONCAT('demo.student.', LPAD(numbers.n, 2, '0')), 0,
           CASE WHEN numbers.n = 20 THEN 0 ELSE 1 END
    FROM (
        SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
        UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12
        UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15 UNION ALL SELECT 16
        UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
    ) AS numbers
) AS seed
WHERE NOT EXISTS (
    SELECT 1 FROM users existing
    WHERE existing.username IN (seed.username, CONCAT(seed.username, '@example.test'))
       OR existing.email IN (seed.username, CONCAT(seed.username, '@example.test'))
);

INSERT INTO admins (created_at, updated_at, admin_code, full_name, usersid)
SELECT @demo_now, @demo_now, 'DEMO-AD001', 'Demo - Quan tri vien', account.id
FROM users account
WHERE account.username = 'demo.admin' AND account.email = 'demo.admin@example.test' AND account.role = 2
  AND NOT EXISTS (
      SELECT 1 FROM admins existing
      WHERE existing.admin_code = 'DEMO-AD001' OR existing.usersid = account.id
  );

INSERT INTO lecturers (created_at, updated_at, lecturer_code, full_name, department, phone, usersid)
SELECT @demo_now, @demo_now, CONCAT('DEMO-GV', LPAD(seed.n, 3, '0')), seed.full_name,
       seed.department, CONCAT('000800', LPAD(seed.n, 4, '0')), account.id
FROM (
    SELECT 1 AS n, 'Demo - Nguyen Minh An' AS full_name, 'Cong nghe thong tin' AS department
    UNION ALL SELECT 2, 'Demo - Tran Thu Binh', 'Khoa hoc may tinh'
    UNION ALL SELECT 3, 'Demo - Le Quang Chau', 'He thong thong tin'
) AS seed
JOIN users account ON account.username = CONCAT('demo.lecturer.', LPAD(seed.n, 2, '0'))
    AND account.email = CONCAT(account.username, '@example.test') AND account.role = 1
WHERE NOT EXISTS (
    SELECT 1 FROM lecturers existing
    WHERE existing.lecturer_code = CONCAT('DEMO-GV', LPAD(seed.n, 3, '0')) OR existing.usersid = account.id
);

INSERT INTO students (created_at, updated_at, student_code, full_name, date_of_birth, phone, usersid)
SELECT @demo_now, @demo_now, CONCAT('DEMO-SV', LPAD(seed.n, 3, '0')), seed.full_name,
       seed.date_of_birth, CONCAT('000900', LPAD(seed.n, 4, '0')), account.id
FROM (
    SELECT 1 AS n, 'Demo - Nguyen Van An' AS full_name, CAST('2004-01-15' AS DATE) AS date_of_birth
    UNION ALL SELECT 2, 'Demo - Tran Thi Binh', '2004-02-20'
    UNION ALL SELECT 3, 'Demo - Le Minh Chau', '2004-03-12'
    UNION ALL SELECT 4, 'Demo - Pham Quang Dung', '2004-04-08'
    UNION ALL SELECT 5, 'Demo - Hoang Thu Ha', '2004-05-23'
    UNION ALL SELECT 6, 'Demo - Vu Minh Hieu', '2004-06-17'
    UNION ALL SELECT 7, 'Demo - Dang Gia Huy', '2004-07-06'
    UNION ALL SELECT 8, 'Demo - Bui Ngoc Khanh', '2004-08-19'
    UNION ALL SELECT 9, 'Demo - Do Phuong Linh', '2004-09-10'
    UNION ALL SELECT 10, 'Demo - Ngo Duc Long', '2004-10-27'
    UNION ALL SELECT 11, 'Demo - Duong Thanh Mai', '2005-01-11'
    UNION ALL SELECT 12, 'Demo - Ly Tuan Nam', '2005-02-14'
    UNION ALL SELECT 13, 'Demo - Truong Bao Ngoc', '2005-03-21'
    UNION ALL SELECT 14, 'Demo - Dinh Hoang Phuc', '2005-04-30'
    UNION ALL SELECT 15, 'Demo - Mai Ngoc Quynh', '2005-05-16'
    UNION ALL SELECT 16, 'Demo - Cao Minh Son', '2005-06-09'
    UNION ALL SELECT 17, 'Demo - Ta Anh Thu', '2005-07-25'
    UNION ALL SELECT 18, 'Demo - Phan Duc Tien', '2005-08-03'
    UNION ALL SELECT 19, 'Demo - Trinh Bao Tram', '2005-09-18'
    UNION ALL SELECT 20, 'Demo - Lam Thanh Tung', '2005-10-07'
) AS seed
JOIN users account ON account.username = CONCAT('demo.student.', LPAD(seed.n, 2, '0'))
    AND account.email = CONCAT(account.username, '@example.test') AND account.role = 0
WHERE NOT EXISTS (
    SELECT 1 FROM students existing
    WHERE existing.student_code = CONCAT('DEMO-SV', LPAD(seed.n, 3, '0'))
       OR existing.phone = CONCAT('000900', LPAD(seed.n, 4, '0')) OR existing.usersid = account.id
);

-- 2. Catalog and classes. These codes do not overlap the older MOCK-* seed.
INSERT INTO subjects (created_at, updated_at, subject_code, subject_name, credits, status)
SELECT @demo_now, @demo_now, seed.code, seed.name, seed.credits, seed.status
FROM (
    SELECT 'DEMO-INT1001' AS code, 'Demo - Lap trinh huong doi tuong' AS name, 3 AS credits, 'ACTIVE' AS status
    UNION ALL SELECT 'DEMO-INT1002', 'Demo - Co so du lieu', 3, 'ACTIVE'
    UNION ALL SELECT 'DEMO-INT1003', 'Demo - Lap trinh web', 4, 'ACTIVE'
    UNION ALL SELECT 'DEMO-INT1004', 'Demo - Cau truc du lieu va giai thuat', 3, 'ACTIVE'
    UNION ALL SELECT 'DEMO-INT1005', 'Demo - Mang may tinh', 3, 'ACTIVE'
    UNION ALL SELECT 'DEMO-INT1006', 'Demo - Tri tue nhan tao', 3, 'INACTIVE'
) AS seed
WHERE NOT EXISTS (
    SELECT 1 FROM subjects existing WHERE existing.subject_code = seed.code OR existing.subject_name = seed.name
);

INSERT INTO semesters (created_at, updated_at, name)
SELECT @demo_now, @demo_now, seed.name
FROM (
    SELECT 'Demo - Hoc ky 2 2025-2026' AS name
    UNION ALL SELECT 'Demo - Hoc ky 1 2026-2027'
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM semesters existing WHERE existing.name = seed.name);

INSERT INTO course_classes
    (created_at, updated_at, class_code, max_students, status, subjectsid, semestersid, lecturersid)
SELECT @demo_now, @demo_now, seed.code, seed.capacity, seed.status, subject.id, semester.id, lecturer.id
FROM (
    SELECT 'DEMO-CLASS-01' AS code, 'DEMO-INT1001' AS subject_code, 'DEMO-GV001' AS lecturer_code,
           'Demo - Hoc ky 1 2026-2027' AS semester_name, 40 AS capacity, 'ACTIVE' AS status
    UNION ALL SELECT 'DEMO-CLASS-02', 'DEMO-INT1002', 'DEMO-GV001', 'Demo - Hoc ky 1 2026-2027', 45, 'ACTIVE'
    UNION ALL SELECT 'DEMO-CLASS-03', 'DEMO-INT1003', 'DEMO-GV002', 'Demo - Hoc ky 1 2026-2027', 50, 'ACTIVE'
    UNION ALL SELECT 'DEMO-CLASS-04', 'DEMO-INT1004', 'DEMO-GV002', 'Demo - Hoc ky 2 2025-2026', 35, 'INACTIVE'
    UNION ALL SELECT 'DEMO-CLASS-05', 'DEMO-INT1005', 'DEMO-GV003', 'Demo - Hoc ky 1 2026-2027', 40, 'ACTIVE'
    UNION ALL SELECT 'DEMO-CLASS-06', 'DEMO-INT1006', 'DEMO-GV003', 'Demo - Hoc ky 1 2026-2027', 30, 'INACTIVE'
) AS seed
JOIN subjects subject ON subject.subject_code = seed.subject_code
JOIN semesters semester ON semester.name = seed.semester_name
JOIN lecturers lecturer ON lecturer.lecturer_code = seed.lecturer_code
WHERE NOT EXISTS (SELECT 1 FROM course_classes existing WHERE existing.class_code = seed.code);

INSERT INTO semester_has_subject (created_at, updated_at, semestersid, subjectsid)
SELECT @demo_now, @demo_now, seed.semestersid, seed.subjectsid
FROM (
    SELECT DISTINCT course.semestersid, course.subjectsid FROM course_classes course
    WHERE course.class_code IN ('DEMO-CLASS-01', 'DEMO-CLASS-02', 'DEMO-CLASS-03',
                                'DEMO-CLASS-04', 'DEMO-CLASS-05', 'DEMO-CLASS-06')
) AS seed
WHERE NOT EXISTS (
    SELECT 1 FROM semester_has_subject existing
    WHERE existing.semestersid = seed.semestersid AND existing.subjectsid = seed.subjectsid
);

-- 3. Enrollments: 46 total, 45 ACTIVE and 1 INACTIVE. Class 06 stays empty.
INSERT INTO student_has_course_class (created_at, updated_at, course_classesid, studentsid, status)
SELECT @demo_now, @demo_now, course.id, student.id,
       CASE WHEN seed.code = 'DEMO-CLASS-05' AND student.student_code = 'DEMO-SV020'
            THEN 'INACTIVE' ELSE 'ACTIVE' END
FROM (
    SELECT 'DEMO-CLASS-01' AS code, 'DEMO-SV001' AS first_student, 'DEMO-SV010' AS last_student
    UNION ALL SELECT 'DEMO-CLASS-02', 'DEMO-SV006', 'DEMO-SV015'
    UNION ALL SELECT 'DEMO-CLASS-03', 'DEMO-SV011', 'DEMO-SV020'
    UNION ALL SELECT 'DEMO-CLASS-04', 'DEMO-SV001', 'DEMO-SV008'
    UNION ALL SELECT 'DEMO-CLASS-05', 'DEMO-SV013', 'DEMO-SV020'
) AS seed
JOIN course_classes course ON course.class_code = seed.code
JOIN students student ON student.student_code BETWEEN seed.first_student AND seed.last_student
    AND REGEXP_LIKE(student.student_code, '^DEMO-SV[0-9]{3}$', 'c')
WHERE NOT EXISTS (
    SELECT 1 FROM student_has_course_class existing
    WHERE existing.course_classesid = course.id AND existing.studentsid = student.id
);

-- 4. Four scenarios per active class: unopened, OPEN, CLOSED, expired but not finalized.
-- session_no is unique inside each class. These high demo values remain useful
-- for deterministic fixture joins, but are NOT primary keys or API path IDs.
INSERT INTO class_sessions
    (created_at, updated_at, course_classesid, session_no, session_date, start_time, end_time)
SELECT @demo_now, @demo_now, course.id, 940000 + seed.class_no * 10 + slot.n,
       CASE slot.n WHEN 1 THEN DATE_ADD(@demo_today, INTERVAL 1 DAY)
                   WHEN 2 THEN DATE(@demo_open_deadline)
                   WHEN 3 THEN DATE_SUB(@demo_today, INTERVAL 7 DAY)
                   ELSE DATE_SUB(@demo_today, INTERVAL 1 DAY) END,
       CASE WHEN slot.n = 1 THEN CAST('14:00:00' AS TIME)
            WHEN slot.n = 2 THEN CASE WHEN HOUR(@demo_open_deadline) >= 2
                                     THEN SUBTIME(TIME(@demo_open_deadline), '02:00:00')
                                     ELSE CAST('00:00:00' AS TIME) END
            ELSE CAST('08:00:00' AS TIME) END,
       CASE WHEN slot.n = 1 THEN CAST('16:00:00' AS TIME)
            WHEN slot.n = 2 THEN TIME(@demo_open_deadline)
            ELSE CAST('10:00:00' AS TIME) END
FROM (
    SELECT 'DEMO-CLASS-01' AS code, 1 AS class_no
    UNION ALL SELECT 'DEMO-CLASS-02', 2
    UNION ALL SELECT 'DEMO-CLASS-03', 3
    UNION ALL SELECT 'DEMO-CLASS-05', 5
) AS seed
CROSS JOIN (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) AS slot
JOIN course_classes course ON course.class_code = seed.code
WHERE NOT EXISTS (
    SELECT 1 FROM class_sessions existing
    WHERE existing.course_classesid = course.id AND existing.session_no = 940000 + seed.class_no * 10 + slot.n
);

-- The inactive historical class gets two completed sessions.
INSERT INTO class_sessions
    (created_at, updated_at, course_classesid, session_no, session_date, start_time, end_time)
SELECT @demo_now, @demo_now, course.id, seed.session_no,
       DATE_SUB(@demo_today, INTERVAL seed.days_ago DAY), '08:00:00', '10:00:00'
FROM (SELECT 940041 AS session_no, 90 AS days_ago UNION ALL SELECT 940042, 83) AS seed
JOIN course_classes course ON course.class_code = 'DEMO-CLASS-04'
WHERE NOT EXISTS (
    SELECT 1 FROM class_sessions existing
    WHERE existing.course_classesid = course.id AND existing.session_no = seed.session_no
);

-- 5. Attendance sessions: 4 OPEN, 6 explicitly CLOSED, 4 expired with stored OPEN.
-- The unopened class sessions deliberately have no attendance_sessions row.
INSERT INTO attendance_sessions
    (created_at, updated_at, class_seesionsid, opened_at, closed_at, late_after_minutes, status)
SELECT @demo_now, @demo_now, lesson.id,
       CASE WHEN seed.kind = 'OPEN'
            THEN LEAST(DATE_SUB(@demo_now, INTERVAL 30 MINUTE), TIMESTAMP(lesson.session_date, lesson.start_time))
            ELSE TIMESTAMP(lesson.session_date, lesson.start_time) END,
       TIMESTAMP(lesson.session_date, lesson.end_time), 15,
       CASE WHEN seed.kind = 'CLOSED' THEN 'CLOSED' ELSE 'OPEN' END
FROM (
    SELECT 'DEMO-CLASS-01' AS code, 940012 AS session_no, 'OPEN' AS kind
    UNION ALL SELECT 'DEMO-CLASS-01', 940013, 'CLOSED'
    UNION ALL SELECT 'DEMO-CLASS-01', 940014, 'EXPIRED'
    UNION ALL SELECT 'DEMO-CLASS-02', 940022, 'OPEN'
    UNION ALL SELECT 'DEMO-CLASS-02', 940023, 'CLOSED'
    UNION ALL SELECT 'DEMO-CLASS-02', 940024, 'EXPIRED'
    UNION ALL SELECT 'DEMO-CLASS-03', 940032, 'OPEN'
    UNION ALL SELECT 'DEMO-CLASS-03', 940033, 'CLOSED'
    UNION ALL SELECT 'DEMO-CLASS-03', 940034, 'EXPIRED'
    UNION ALL SELECT 'DEMO-CLASS-04', 940041, 'CLOSED'
    UNION ALL SELECT 'DEMO-CLASS-04', 940042, 'CLOSED'
    UNION ALL SELECT 'DEMO-CLASS-05', 940052, 'OPEN'
    UNION ALL SELECT 'DEMO-CLASS-05', 940053, 'CLOSED'
    UNION ALL SELECT 'DEMO-CLASS-05', 940054, 'EXPIRED'
) AS seed
JOIN course_classes course ON course.class_code = seed.code
JOIN class_sessions lesson ON lesson.course_classesid = course.id AND lesson.session_no = seed.session_no
WHERE NOT EXISTS (SELECT 1 FROM attendance_sessions existing WHERE existing.class_seesionsid = lesson.id);

-- 6. Attendance: 77 rows on a first import (43 PRESENT, 25 LATE, 9 ABSENT).
-- CLOSED sessions cover every ACTIVE enrollee. OPEN/expired sessions are partial
-- so manual recording and /close can be exercised. Method=1, confidence=0.
INSERT INTO attendance
    (created_at, updated_at, attendnce_sessionsid, studentsid, recognized_at, status, method, confidence)
SELECT @demo_now, @demo_now, seed.attendance_session_id, seed.student_id,
       CASE seed.attendance_status
            WHEN 'ABSENT' THEN seed.closed_at
            WHEN 'LATE' THEN DATE_ADD(seed.opened_at, INTERVAL 20 MINUTE)
            ELSE DATE_ADD(seed.opened_at, INTERVAL 5 MINUTE) END,
       seed.attendance_status, 1, 0.0000
FROM (
    SELECT DISTINCT attendance_session.id AS attendance_session_id, student.id AS student_id,
           attendance_session.opened_at, attendance_session.closed_at,
           CASE WHEN layout.kind = 'CLOSED' THEN
                    CASE WHEN MOD(CAST(RIGHT(student.student_code, 3) AS UNSIGNED), 5) = 0 THEN 'ABSENT'
                         WHEN MOD(CAST(RIGHT(student.student_code, 3) AS UNSIGNED), 3) = 0 THEN 'LATE'
                         ELSE 'PRESENT' END
                WHEN MOD(CAST(RIGHT(student.student_code, 3) AS UNSIGNED) - layout.first_student + 1, 2) = 0
                     THEN 'LATE'
                ELSE 'PRESENT' END AS attendance_status
    FROM (
        SELECT 'DEMO-CLASS-01' AS code, 940012 AS session_no, 'OPEN' AS kind, 1 AS first_student, 4 AS last_student
        UNION ALL SELECT 'DEMO-CLASS-01', 940013, 'CLOSED', 1, 10
        UNION ALL SELECT 'DEMO-CLASS-01', 940014, 'EXPIRED', 1, 2
        UNION ALL SELECT 'DEMO-CLASS-02', 940022, 'OPEN', 6, 9
        UNION ALL SELECT 'DEMO-CLASS-02', 940023, 'CLOSED', 6, 15
        UNION ALL SELECT 'DEMO-CLASS-02', 940024, 'EXPIRED', 6, 7
        UNION ALL SELECT 'DEMO-CLASS-03', 940032, 'OPEN', 11, 14
        UNION ALL SELECT 'DEMO-CLASS-03', 940033, 'CLOSED', 11, 20
        UNION ALL SELECT 'DEMO-CLASS-03', 940034, 'EXPIRED', 11, 12
        UNION ALL SELECT 'DEMO-CLASS-04', 940041, 'CLOSED', 1, 8
        UNION ALL SELECT 'DEMO-CLASS-04', 940042, 'CLOSED', 1, 8
        UNION ALL SELECT 'DEMO-CLASS-05', 940052, 'OPEN', 13, 16
        UNION ALL SELECT 'DEMO-CLASS-05', 940053, 'CLOSED', 13, 20
        UNION ALL SELECT 'DEMO-CLASS-05', 940054, 'EXPIRED', 13, 14
    ) AS layout
    JOIN course_classes course ON course.class_code = layout.code
    JOIN class_sessions lesson ON lesson.course_classesid = course.id AND lesson.session_no = layout.session_no
    JOIN attendance_sessions attendance_session ON attendance_session.class_seesionsid = lesson.id
    JOIN student_has_course_class enrollment ON enrollment.course_classesid = course.id AND enrollment.status = 'ACTIVE'
    JOIN students student ON student.id = enrollment.studentsid
        AND student.student_code BETWEEN CONCAT('DEMO-SV', LPAD(layout.first_student, 3, '0'))
                                     AND CONCAT('DEMO-SV', LPAD(layout.last_student, 3, '0'))
        AND REGEXP_LIKE(student.student_code, '^DEMO-SV[0-9]{3}$', 'c')
) AS seed
WHERE NOT EXISTS (
    SELECT 1 FROM attendance existing
    WHERE existing.attendnce_sessionsid = seed.attendance_session_id AND existing.studentsid = seed.student_id
);

COMMIT;

-- These SELECTs are part of the manual script, not queries run by the assistant.
-- Generated IDs below are the values to use in API paths; never use session_no as {id}.
SELECT course.id AS class_id, course.class_code, lecturer.lecturer_code, account.username AS lecturer_username
FROM course_classes course
JOIN lecturers lecturer ON lecturer.id = course.lecturersid
JOIN users account ON account.id = lecturer.usersid
WHERE course.class_code IN ('DEMO-CLASS-01', 'DEMO-CLASS-02', 'DEMO-CLASS-03',
                            'DEMO-CLASS-04', 'DEMO-CLASS-05', 'DEMO-CLASS-06')
ORDER BY course.class_code;

SELECT course.class_code, lesson.id AS class_session_id, lesson.session_no, lesson.session_date,
       attendance_session.id AS attendance_session_id, attendance_session.status AS stored_status,
       CASE WHEN attendance_session.id IS NULL THEN 'NOT_OPENED'
            WHEN attendance_session.status = 'CLOSED' OR attendance_session.closed_at <= CURRENT_TIMESTAMP(6)
                 THEN 'CLOSED' ELSE 'OPEN' END AS effective_status
FROM class_sessions lesson
JOIN course_classes course ON course.id = lesson.course_classesid
LEFT JOIN attendance_sessions attendance_session ON attendance_session.class_seesionsid = lesson.id
WHERE course.class_code IN ('DEMO-CLASS-01', 'DEMO-CLASS-02', 'DEMO-CLASS-03', 'DEMO-CLASS-04', 'DEMO-CLASS-05')
  AND lesson.session_no IN (940011, 940012, 940013, 940014, 940021, 940022, 940023, 940024,
                            940031, 940032, 940033, 940034, 940041, 940042, 940051, 940052, 940053, 940054)
ORDER BY course.class_code, lesson.session_no;

SELECT student.id AS student_id, student.student_code, student.full_name, account.username, account.status
FROM students student
JOIN users account ON account.id = student.usersid
WHERE REGEXP_LIKE(student.student_code, '^DEMO-SV(00[1-9]|01[0-9]|020)$', 'c')
ORDER BY student.student_code;
