# Request DTOs

Payloads follow the API document's examples, except that entity statuses now use
the shared INACTIVE/ACTIVE enum. Where the document does not define a payload,
fields are based on the current entities. IDs already in the URL stay in path
variables. GET filters (including attendance `classId`) use query parameters.

| Endpoint | Request DTO |
| --- | --- |
| `POST /api/auth/login` | `LoginRequest` |
| `POST /api/admin/students` | `CreateStudentRequest` |
| `PATCH /api/admin/students/{id}` | `UpdateStudentRequest` |
| `PATCH /api/admin/students/{id}/status` | `UpdateStudentStatusRequest` |
| `POST /api/admin/lecturers` | `CreateLecturerRequest` |
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

Services must assign initial statuses, roles, session numbers, timestamps and
attendance methods; encode passwords; and check uniqueness, resource ownership
and referenced records. Student status belongs to its linked User. All entity
status fields and corresponding request fields use `com.example.oopPTIT.util.Status`
with JSON values "INACTIVE" or "ACTIVE". The status update requests require a
value; subject PATCH allows it to be omitted. MANUAL is an attendance method.

Attendance and attendance session statuses also use this shared enum, replacing
the original PRESENT/LATE/ABSENT and OPEN/CLOSED contract. Existing database rows
with other status values must be migrated before loading them through these entities.

`CreateCourseClassRequest.semesterId` follows the API contract, but CourseClass
currently has no semester relationship. Add that mapping when implementing class
creation. DTO validation does not implement persistence or business workflows.
