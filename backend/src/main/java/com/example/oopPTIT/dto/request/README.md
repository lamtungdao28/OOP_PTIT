# Request DTOs

Payloads follow the API document's examples. Account/enrollment statuses use
INACTIVE/ACTIVE; attendance statuses use PRESENT/LATE/ABSENT. Where the document does not define a payload,
fields are based on the current entities. IDs already in the URL stay in path
variables. GET pagination and filters use query parameters.

| Endpoint | Request DTO |
| --- | --- |
| `POST /api/auth/login` | `LoginRequest` |
| `GET /api/auth/me` | No body; verified Bearer token |
| `GET /api/admin/users` | `Pageable`; ADMIN only |
| `GET /api/lecturer/classes` | `Pageable` and optional `String[] courseClass` |
| `POST /api/lecturer/classes/{classId}/attendance` | `RecordAttendanceRequest`; assigned lecturer or ADMIN |
| `GET /api/student/profile` | No body; identity from JWT |
| `GET /api/student/attendance` | `Pageable`; identity from JWT |
| `POST /api/admin/students` | `CreateStudentRequest` |
| `GET /api/admin/students` | `Pageable` and `String[] student` (query parameters) |
| `PATCH /api/admin/students/{id}` | `UpdateStudentRequest` |
| `PATCH /api/admin/students/{id}/status` | `UpdateStudentStatusRequest` |
| `POST /api/admin/lecturers` | `CreateLecturerRequest` |
| `GET /api/admin/lecturers` | `Pageable` and `String[] lecturer` (query parameters) |
| `GET /api/admin/lecturers/{id}` | No body; lecturer ID in the path |
| `PATCH /api/admin/lecturers/{id}` | `UpdateLecturerRequest` |
| `POST /api/admin/subjects` | `CreateSubjectRequest` |
| `PATCH /api/admin/subjects/{id}` | `UpdateSubjectRequest` |
| `POST /api/admin/semesters` | `CreateSemesterRequest` |
| `POST /api/admin/classes` | `CreateCourseClassRequest` |
| `PATCH /api/admin/classes/{id}/lecturer` | `AssignLecturerRequest` |
| `POST /api/admin/classes/{id}/enrollments` | `EnrollStudentRequest` |
| `DELETE /api/admin/classes/{id}/enrollments/{studentId}` | No body; class and student IDs in the path |
| `GET /api/students/me/classes` | `Pageable` and optional `String[] courseClass`; identity from the authenticated Principal |
| `POST/PUT /api/students/me/face-profile` | Multipart `image` part; authenticated STUDENT |
| `GET /api/classes/{id}/sessions` | `Pageable`; assigned lecturer or ADMIN; default sort `sessionNo,asc` |
| `POST /api/classes/{id}/sessions` | `CreateClassSessionRequest` exists, but endpoint is not implemented |
| `POST /api/sessions/{id}/attendance/open` | `OpenAttendanceSessionRequest`; assigned LECTURER only |
| `GET /api/sessions/{id}/attendance` | No body; assigned LECTURER only; path ID is the ClassSession ID |
| `POST /api/sessions/{id}/attendance/recognize` | Multipart `image` part; assigned LECTURER only; path ID is the ClassSession ID |
| `POST /api/sessions/{id}/attendance/manual` | `ManualAttendanceRequest`; assigned LECTURER only |
| `PATCH /api/attendance/{id}` | `UpdateAttendanceRequest`; Attendance record ID; assigned LECTURER only |
| `POST /api/sessions/{id}/attendance/close` | No body; assigned LECTURER only |
| `GET /api/sessions/{id}/attendance/results` | `Pageable`; assigned LECTURER only; default sort `student.studentCode,asc` |

Use `@Valid @RequestBody` for JSON. Face-profile registration/replacement and
recognition accept multipart/form-data with an `image` part via `@RequestPart`;
the existing image request DTOs are not used by these controllers. Student
identity comes from JWT. Uploads accept JPEG, PNG or WebP up to 5 MB; FastAPI
decodes the image and requires exactly one clear face. No request body is needed
for closing attendance, deleting an enrollment or the listed GET endpoints.

For profile and subject PATCH requests, omitted or null fields mean unchanged;
the service must skip them when applying updates. Blank supplied strings are
invalid. Account credentials are accepted only on creation and login.

Student search follows Subject's `Pageable` + SpecificationBuilder pattern.
Repeat the optional `student` query parameter for multiple filters; they are
combined with AND. Each filter uses `field` + `operator` + `value`. Supported
fields are `id`, `fullName`, `studentCode`, `phone`, `dateOfBirth`, `username`,
`email` and `status`. The last three fields belong to the linked User.

Operators are `:` (equal), `!` (not equal), `>` (strictly greater/after), `<`
(strictly less/before) and `~` (contains text). Text equality also supports
`field:value*` (starts with), `field:*value` (ends with), and `field:*value*`
(contains). Text matching is case-insensitive and trims outer whitespace; SQL
LIKE metacharacters `%`, `_` and `!` are literal. Dates use `yyyy-MM-dd`, IDs
are integers, and status accepts `ACTIVE`/`INACTIVE` with `:` or `!` only.
Missing or blank filter parameters are ignored. Invalid fields, operators,
values, or unsupported sort fields return HTTP 400.

Pagination uses zero-based `page`, `size` (default 20), and `sort=field,asc|desc`
(default `id,asc`). Sort fields are `id`, `studentCode`, `fullName`, `dateOfBirth`
and `phone`. `data` is now a ResponsePage containing `pageNo`, `pageSize`,
`totalPage` and `items` (StudentResponse objects, without account credentials).
Clients must read `data.items` instead of `data` and migrate old named filters
such as `fullName=Nguyen` to `student=fullName~Nguyen`.

```http
GET /api/admin/students?page=0&size=10&sort=studentCode,asc
GET /api/admin/students?student=fullName~Nguyen&student=studentCode:B23*&student=status:ACTIVE
GET /api/admin/students?student=dateOfBirth%3E2004-01-01&student=dateOfBirth%3C2007-01-01
```

Lecturer search uses the same pagination, operators, wildcard syntax, AND
combination and validation as student search, with repeated `lecturer` query
parameters. Supported fields are `id`, `lecturerCode`, `fullName`, `department`,
`phone`, `username`, `email` and `status`; the last three belong to the linked
User. Sort fields are `id`, `lecturerCode`, `fullName`, `department` and `phone`.
The defaults are `page=0`, `size=20` and `sort=id,asc`. Its response now places
LecturerResponse objects in `data.items` instead of the previous `data` array,
alongside `pageNo`, `pageSize` and `totalPage`. Passwords and entity relationships
are not included in the response.

```http
GET /api/admin/lecturers?page=0&size=10&sort=lecturerCode,asc
GET /api/admin/lecturers?lecturer=fullName~Nguyen&lecturer=department:CNTT&lecturer=status:ACTIVE
GET /api/admin/lecturers?lecturer=lecturerCode:GV*&lecturer=email~ptit.edu.vn
```

`GET /api/admin/lecturers/{id}` returns one LecturerResponse in `data`, including
the linked account's username, email and status but never its password. A missing
lecturer returns HTTP 404; an ID that cannot be parsed as a Long returns HTTP 400.

`PATCH /api/admin/classes/{id}/lecturer` assigns or replaces a class's lecturer.
The body requires a positive `lecturerId`; the class and lecturer must both exist.
Success returns HTTP 200 with the updated CourseClassResponse in `data`, including
`lecturerId` and `updatedAt`. Other class fields remain unchanged. Assigning the
same lecturer again is allowed. Missing references return HTTP 404; missing,
null, non-positive or malformed lecturer IDs return HTTP 400. This endpoint
does not remove the lecturer assignment.

```http
PATCH /api/admin/classes/12/lecturer
Content-Type: application/json

{"lecturerId": 52}
```

`DELETE /api/admin/classes/{id}/enrollments/{studentId}` removes the student's
enrollment records for that class, including any duplicates. It does not delete
the student, their account, attendance records or enrollment in other classes.
No request body is needed. Success returns HTTP 200 with a status and message,
without `data`. Missing classes, students or enrollments return HTTP 404,
including repeated deletion after the enrollment has been removed. Malformed
path IDs return HTTP 400. ADMIN authorization is enforced by Spring Security
at both the HTTP and service layers.

```http
DELETE /api/admin/classes/12/enrollments/34
```

`GET /api/students/me/classes` returns the current student's registered classes
as CourseClassResponse objects in `data.items`, with `pageNo`, `pageSize` and
`totalPage`. Pagination defaults to `page=0`, `size=20`, `sort=id,asc`.
Repeat `courseClass` to combine filters with AND, using the same fields and
operators as the admin class list, for example `semesterId:1`, `status:ACTIVE`
or `classCode~INT2204`. Filters never expand the result beyond the student's
own enrollments. Duplicate enrollments do not duplicate classes or page counts.
A student with no enrollments receives an empty page.

Identity is resolved from `Principal.getName()`, which must be the authenticated
User's username. The endpoint does not accept a student ID or username from
query parameters or headers. Missing authentication returns HTTP 401; a missing
student profile, non-STUDENT role or INACTIVE account returns HTTP 403.
The Resource Server validates the Bearer JWT and supplies this Principal.
Inactive accounts or tokens with outdated roles are rejected with HTTP 401
before controller invocation. See `docs/SECURITY.md` for login and configuration.

```http
GET /api/students/me/classes?page=0&size=10&sort=classCode,asc&courseClass=semesterId:1
```

Services must assign initial statuses, roles, session numbers, timestamps and
attendance methods; encode passwords; and check uniqueness, resource ownership
and referenced records. Student status belongs to its linked User. Account and
enrollment status fields use `Status` with JSON values "INACTIVE" or "ACTIVE".
The status update requests require a value; subject PATCH allows it to be omitted.

Attendance uses `TimeStatus` (PRESENT/LATE/ABSENT), and attendance sessions use
`SessionStatus` (OPEN/CLOSED). `RecordAttendanceRequest`, `ManualAttendanceRequest`
and `UpdateAttendanceRequest` all use `TimeStatus`; ACTIVE/INACTIVE are invalid
for attendance writes. No attendance schema migration is needed.

`POST /api/sessions/{id}/attendance/open` reuses `OpenAttendanceSessionRequest`:

```json
{"lateAfterMinutes":15}
```

The value is required and must be an integer >= 0. The path ID is the class
session ID, not the course class ID or attendance session ID. The service sets
the opening time and the deadline (`sessionDate` + `endTime`). Opening an ended
class session or one that already has an attendance session returns 409.
Success returns HTTP 201 with `AttendanceSessionResponse` in `data`.

`GET /api/sessions/{id}/attendance` returns HTTP 200 with the same DTO: `id`,
`classSessionId`, `classId`, `openedAt`, `closedAt`, `lateAfterMinutes`, and
effective `status` (CLOSED after the deadline). Before the first successful open,
GET returns 404 and does not create a record. Both endpoints require the assigned
LECTURER; ADMIN and STUDENT receive 403. See `docs/SECURITY.md` for lifecycle rules.

`POST /api/sessions/{id}/attendance/manual` accepts:

```json
{"studentId":456,"status":"PRESENT"}
```

Both fields are required; `studentId` must be positive. Status may also be LATE
or ABSENT. The student needs an ACTIVE enrollment in the session's class.
Success returns 201 with `AttendanceResponse`; duplicates or closed/expired
sessions return 409. The backend sets method 1, confidence 0 and recording time.

`PATCH /api/attendance/{id}` accepts `{"status":"LATE"}` and returns 200 with
`AttendanceResponse`. Here `{id}` is the attendance record ID. Corrections are
allowed after closure and only change status, preserving the other record fields.

`POST /api/sessions/{id}/attendance/close` takes no body, fills missing ABSENT
records for distinct ACTIVE enrollees, and returns 200 with the closed session.
It preserves existing records and is idempotent. Expired sessions can still be
finalized; missing/unopened sessions return 404.

`GET /api/sessions/{id}/attendance/results` returns `data.session`, `data.statistics`
(`recorded`, `present`, `late`, `absent`, `unmarked`) and `data.attendance`
(`pageNo`, `pageSize`, `totalPage`, `items`). Statistics cover the whole session;
items contain existing attendance records and student ID/code/name only, with
status/time/method. `unmarked` counts currently ACTIVE students with no record.
Default pagination is page 0, size 20, sort `student.studentCode,asc`.
All four endpoints above require the assigned LECTURER. The recognition endpoint
is implemented separately with multipart image input; see the repository root
README and `face-service/README.md`. The earlier manual-attendance examples in
`docs/SECURITY.md` predate the face-recognition integration.

`POST /api/admin/classes` stores the semester through `CourseClass.semester`
(`COURSE_CLASSES.SEMESTERSid`). The service checks that the subject, semester and
lecturer exist, assigns `ACTIVE`, and returns the new class ID. Existing class rows
must be assigned their actual semester before enforcing the non-null foreign key.
DTO validation does not implement persistence or business workflows.
