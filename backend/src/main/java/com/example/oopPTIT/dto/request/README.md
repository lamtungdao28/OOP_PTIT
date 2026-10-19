# Request DTOs

Payloads follow the API document's examples, except that entity statuses now use
the shared INACTIVE/ACTIVE enum. Where the document does not define a payload,
fields are based on the current entities. IDs already in the URL stay in path
variables. GET filters (including attendance `classId`) use query parameters.

| Endpoint | Request DTO |
| --- | --- |
| `POST /api/auth/login` | `LoginRequest` |
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
| `POST/PUT /api/students/me/face-profile` | `FaceProfileRequest` |
| `POST /api/classes/{id}/sessions` | `CreateClassSessionRequest` |
| `POST /api/sessions/{id}/attendance/open` | `OpenAttendanceSessionRequest` |
| `POST /api/sessions/{id}/attendance/recognize` | `RecognizeAttendanceRequest` |
| `POST /api/sessions/{id}/attendance/manual` | `ManualAttendanceRequest` |
| `PATCH /api/attendance/{id}` | `UpdateAttendanceRequest` |

Use `@Valid @RequestBody` for JSON. The two image DTOs use multipart/form-data
with an `image` part and `@Valid @ModelAttribute`; student identity comes from JWT.
Image decoding, allowed formats and upload limits must be enforced when adding
the upload endpoints. No request body is needed for closing attendance, deleting
an enrollment or the listed GET endpoints.

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

Services must assign initial statuses, roles, session numbers, timestamps and
attendance methods; encode passwords; and check uniqueness, resource ownership
and referenced records. Student status belongs to its linked User. All entity
status fields and corresponding request fields use `com.example.oopPTIT.util.Status`
with JSON values "INACTIVE" or "ACTIVE". The status update requests require a
value; subject PATCH allows it to be omitted. MANUAL is an attendance method.

Attendance and attendance session statuses also use this shared enum, replacing
the original PRESENT/LATE/ABSENT and OPEN/CLOSED contract. Existing database rows
with other status values must be migrated before loading them through these entities.

`POST /api/admin/classes` stores the semester through `CourseClass.semester`
(`COURSE_CLASSES.SEMESTERSid`). The service checks that the subject, semester and
lecturer exist, assigns `ACTIVE`, and returns the new class ID. Existing class rows
must be assigned their actual semester before enforcing the non-null foreign key.
DTO validation does not implement persistence or business workflows.
