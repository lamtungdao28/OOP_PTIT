# JWT Authentication and Authorization

## 1. Existing Architecture and Assumptions

The application uses Java 21, Spring Boot 4.1.1, Spring MVC, Spring Data JPA and
MySQL. Boot's dependency management selects Spring Security 7.1.1. The security
starters supply Nimbus JWT support; no independent JWT version or custom Bearer
filter is needed.

The implementation follows Controller -> Service -> Repository and reuses these
entities without schema changes:

- `USERS`: username, email, password hash, role and account status.
- `STUDENTS` and `LECTURERS`: each references one User through `USERSid`.
- `COURSE_CLASSES`: references its lecturer, subject and semester.
- `STUDENT_HAS_COURSE_CLASS`: links registered students to classes.
- `CLASS_SESSIONS` -> `ATTENDANCE_SESSIONS` -> `ATTENDANCE`: existing attendance model.

`User.role` remains ordinal: STUDENT=0, LECTURER=1, ADMIN=2. Do not reorder the
enum or add `EnumType.STRING` without a separate, reviewed data migration.
No roles table, duplicate User entity, Flyway migration, default admin account,
registration endpoint or role-update endpoint is added. Existing admin APIs
still assign STUDENT/LECTURER on account creation; clients cannot supply a role.

Schema analysis is based on the source mappings, not a connection to the live
database. Integration tests use isolated H2 databases. Existing data must already
match the JPA mappings, including the current attendance status enums.

The sample attendance POST is defined as **manual recording for an existing,
OPEN, unexpired attendance session**. `attendanceSessionId` is an AttendanceSession ID,
not a ClassSession ID. Its class must match the path, and the target student must
have an ACTIVE enrollment. The existing integer method column uses `1` for this
manual operation; confidence is `0` because no recognition model is involved.
Confirm this convention before integrating a separate face-recognition system.
Class session creation and AI recognition are outside these endpoints.

## 2. Security Design

- Login accepts a username or email in the existing `username` field.
- A `DaoAuthenticationProvider` and `CustomUserDetailsService` verify credentials.
- New password hashes use BCrypt with cost 12. The existing PBKDF2 v5.8 hashes
  are still accepted and upgraded after successful login, with a conditional
  database update to avoid overwriting a concurrent password change.
- BCrypt passwords cannot exceed 72 UTF-8 bytes. Longer legacy PBKDF2 passwords
  continue to work without truncation or automatic conversion; reset them to a
  BCrypt-compatible password through a trusted administrative workflow.
- Login fails with a generic 401 for incorrect credentials, inactive accounts,
  missing users or ambiguous username/email matches. New student/lecturer
  creation and email updates also check for cross-field identity collisions.
- JWTs use HS256, with a Base64-encoded random key of at least 32 bytes supplied
  through `JWT_SECRET`. The application fails startup if key configuration is
  missing, malformed or too short. The secret is redacted from configuration
  diagnostic strings.
- Claims: `sub` (stable User ID), `userId`, `username`, `roles`, `iss`, `aud`,
  `iat`, `nbf`, `exp`, `jti`. No password, password hash or email is in the JWT.
- Resource Server verifies the signature and pinned algorithm, issuer, audience
  and timestamps. Expiration has no grace period; synchronize server clocks.
- After cryptographic validation, the converter loads the User by verified
  subject and checks account status, username, userId and roles against the DB.
  Disabled/deleted accounts and outdated roles cannot keep using old tokens.
- Authorities are `ROLE_ADMIN`, `ROLE_LECTURER`, `ROLE_STUDENT`. Expressions use
  `hasRole('ADMIN')`, not `hasRole('ROLE_ADMIN')`.
- `@EnableMethodSecurity` protects service entry points. URL rules and controller
  annotations add a second layer; service ownership checks remain essential.
- `/me`, profile and attendance queries use the authenticated identity. Query
  parameters, identity headers and client-provided user IDs cannot override it.
- Class authorization follows `CourseClass.lecturer.user.id`. Attendance writes
  lock the class row, recheck assignment inside the transaction and reject
  duplicate student/session records with 409. ADMIN bypasses assignment checks
  on existing class/manual-recording APIs, but not enrollment/session validity
  checks. Session-based attendance endpoints and attendance corrections are
  assigned-LECTURER-only. Manual recording, correction and closing use READ_COMMITTED so
  reads after waiting for the class lock see the preceding writer's changes,
  including when MySQL otherwise defaults to REPEATABLE_READ.
  Results use a read-only REPEATABLE_READ snapshot for consistent pages and statistics.
- Sessions, form login and HTTP Basic are disabled. Security context and request
  cache are not persisted in HTTP sessions. Only the Authorization Bearer header
  is accepted, not cookies or query-string tokens.
- CSRF is disabled for this stateless, header-token API. Revisit that decision if
  cookie authentication is introduced later.
- CORS allows only explicitly configured frontend origins, required methods and
  Authorization/Content-Type/Accept headers. Wildcard origins are rejected;
  credentialed cookies are not allowed.

Reference documentation:
[Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html),
[method security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html),
[password storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html).

## 3. Files

All Java paths below are relative to `src/main/java/com/example/oopPTIT/`.

New files:

```text
config/SecurityConfig.java
config/OpenApiConfig.java
security/AuthenticatedUser.java
security/CompatiblePasswordEncoder.java
security/CustomUserDetailsService.java
security/DatabaseJwtAuthenticationConverter.java
security/CurrentUser.java
security/CourseAccess.java
security/JwtProperties.java
security/SecurityErrorHandler.java
controller/AuthController.java
controller/AdminUserController.java
controller/LecturerCourseClassController.java
controller/StudentController.java
service/AuthService.java
service/AdminUserService.java
service/AttendanceService.java
service/StudentSelfService.java
repository/AttendanceRepository.java
repository/AttendanceSessionRepository.java
dto/request/RecordAttendanceRequest.java
dto/response/LoginResponse.java
dto/response/CurrentUserResponse.java
dto/response/AttendanceResponse.java
dto/response/AttendanceSessionResponse.java
dto/response/AttendanceResultResponse.java
dto/response/AttendanceResultsResponse.java
dto/response/AttendanceStatisticsResponse.java
```

Updated files include `pom.xml`, `config/PasswordConfig.java`, `LoginRequest`,
User/Student/CourseClass/enrollment repositories, the five existing services,
admin controllers, `CourseClassController`, `StudentCourseClassController`,
`StudentExceptionHandler`, `application.properties` and request documentation.
`.env.example` documents configuration. `.gitignore` no longer excludes tests.
Existing security-independent tests now declare their test identities explicitly.

## 4. Endpoints and Responses

| Method and path | Access |
| --- | --- |
| `POST /api/auth/login` | Public; no Bearer header required |
| `GET /api/auth/me` | Any valid authenticated account |
| `GET /api/admin/users` | ADMIN |
| Existing `/api/admin/**` APIs | ADMIN |
| `GET /api/lecturer/classes` | LECTURER: assigned classes; ADMIN: all classes |
| `GET /api/classes/{id}/students` | Assigned LECTURER or ADMIN |
| `GET /api/classes/{id}/sessions` | Assigned LECTURER or ADMIN |
| `POST /api/sessions/{id}/attendance/open` | Assigned LECTURER only |
| `GET /api/sessions/{id}/attendance` | Assigned LECTURER only |
| `POST /api/sessions/{id}/attendance/manual` | Assigned LECTURER only |
| `PATCH /api/attendance/{id}` | Assigned LECTURER only |
| `POST /api/sessions/{id}/attendance/close` | Assigned LECTURER only |
| `GET /api/sessions/{id}/attendance/results` | Assigned LECTURER only |
| `POST /api/lecturer/classes/{classId}/attendance` | Assigned LECTURER or ADMIN |
| `GET /api/student/profile` | STUDENT: own profile |
| `GET /api/student/attendance` | STUDENT: own attendance |
| `GET /api/students/me/classes` | STUDENT: own enrolled classes |

The plural `/api/students/me/classes` route remains unchanged. ADMIN uses the
admin endpoints for other users' data rather than impersonating a student on
self-service endpoints. STUDENT cannot read the class roster, since it contains
other students' personal information.

List APIs use zero-based `page`, `size` and `sort`, and return `ResponsePage` in
`data` with `pageNo`, `pageSize`, `totalPage`, `items`. Class lists still support
repeated `courseClass` Specification filters and pass Pageable directly through.
Class sort fields are `id`, `classCode`, `maxStudents`, `status`, `createdAt`,
`updatedAt`, `subject.id`, `semester.id`, `lecturer.id`. Roster sort fields are
`id`, `studentCode`, `fullName`, `dateOfBirth`, `phone`. Other fields return 400;
sorting by related account secrets is not permitted.

Class session lists use `GET /api/classes/{id}/sessions?page=0&size=20&sort=sessionNo,asc`.
The path ID is the course class ID. Defaults are page 0, size 20, and ascending
`sessionNo`. Supported sort fields are `id`, `sessionNo`, `sessionDate`,
`startTime`, `endTime`, `createdAt`, and `updatedAt`. Each item contains those
session fields plus `classId`; linked entities and account details are omitted.
An existing class without sessions returns HTTP 200 with an empty `items` list.
ADMIN receives 404 for a nonexistent class; an unassigned lecturer receives 403
without disclosing whether the class exists. Ownership is enforced in the
service as well as role protection on the controller.

### Attendance Session Lifecycle

All `/api/sessions/{id}/attendance/**` endpoints require a LECTURER token whose
account is assigned to the class. ADMIN and STUDENT receive 403. The path `{id}`
is a **ClassSession ID**, obtained from `GET /api/classes/{id}/sessions`, not an AttendanceSession ID.
Client-supplied lecturer IDs, user IDs or class IDs cannot change ownership.

```http
POST /api/sessions/{{classSessionId}}/attendance/open
Authorization: Bearer {{lecturerToken}}
Content-Type: application/json

{"lateAfterMinutes":15}
```

`lateAfterMinutes` is required and must be a nonnegative integer; 0 is valid.
The existing DTO supplies validation. The service sets `openedAt` to server time
and `status` to OPEN. Since the existing `closed_at` column is non-null and the
DTO has no duration, `closedAt` is the scheduled end of the class session
(`sessionDate` + `endTime`, using the server's local time zone). Opening before
the scheduled start is allowed, but opening at or after its end returns 409.
No schema changes or additional request fields are needed.

Success is HTTP 201. `data` contains `id` (AttendanceSession ID), `classSessionId`,
`classId`, `openedAt`, `closedAt`, `lateAfterMinutes`, and `status`. Use this returned
`id` for the existing manual recording API's `attendanceSessionId` field.
Each class session has one attendance session. Repeated or concurrent opens
return 409 without resetting existing timestamps, settings or attendance records;
closed sessions cannot be reopened by this endpoint. The class row is locked
and ownership is checked inside the write transaction.

```http
GET /api/sessions/{{classSessionId}}/attendance
Authorization: Bearer {{lecturerToken}}
```

Success is HTTP 200 with the same response DTO. Effective status is CLOSED when
explicitly closed or when `closedAt` has been reached, even if the stored status
is still OPEN. GET does not update the database; no background scheduler is
introduced. Manual recording uses the same deadline check and rejects expired
sessions with 409. An assigned class session with no attendance session returns
404 without creating one. Missing class sessions return 404; a session belonging
to another lecturer returns 403; malformed IDs or invalid bodies return 400.

### Manual Attendance and Corrections

```http
POST /api/sessions/{{classSessionId}}/attendance/manual
Authorization: Bearer {{lecturerToken}}
Content-Type: application/json

{"studentId":456,"status":"PRESENT"}
```

`ManualAttendanceRequest` requires a positive student ID and a status of
PRESENT, LATE or ABSENT. The student must have an ACTIVE enrollment in this
session's class, and the attendance session must be open and unexpired.
Success returns HTTP 201 with the existing `AttendanceResponse` in `data`.
Duplicate student/session submissions return 409, including duplicates made
through the older class-based manual endpoint. An unenrolled student returns
404. Timestamp, method (1) and confidence (0) are set by the server; identity
and session IDs supplied in extra request fields cannot override the path.
The lecturer chooses the status; `lateAfterMinutes` does not override this
explicit manual decision.

```http
PATCH /api/attendance/{{attendanceId}}
Authorization: Bearer {{lecturerToken}}
Content-Type: application/json

{"status":"LATE"}
```

Here the path ID is the **Attendance record ID**, not either session ID.
`UpdateAttendanceRequest` accepts the same three statuses. Success is HTTP 200
with `AttendanceResponse`; missing records return 404. Only the assigned
LECTURER can correct a record, even after the session is closed or the student's
enrollment has changed. Only status and the entity's update timestamp change;
student, session, recognition time, method and confidence are preserved.
ACTIVE/INACTIVE are account/enrollment statuses and return 400 for these DTOs.

### Closing and Results

```http
POST /api/sessions/{{classSessionId}}/attendance/close
Authorization: Bearer {{lecturerToken}}
```

No body is needed. Within one transaction, the service creates ABSENT records
for distinct ACTIVE enrollees without a record in this attendance session,
preserves all existing results, and marks the session CLOSED. Duplicate
enrollments do not produce duplicate absences, and attendance in another session
does not count as attendance here. Lecturer-triggered absence finalization uses
the existing manual method code 1 and confidence 0. Recognition time is the
closing time, which is the earlier of now and the original deadline.

An expired session can still be finalized to fill the missing ABSENT records;
its deadline is not extended. Repeating an explicit close returns 200 with the
same `AttendanceSessionResponse`, without adding more records or changing the
closing timestamp. In particular, students enrolled after explicit closure
are not retroactively added by retrying close. A session that has never been
opened returns 404. Manual recording after closure returns 409; PATCH remains
available for corrections. Concurrent closes or manual/close requests share
the class lock, so they cannot create duplicate attendance through these APIs.

```http
GET /api/sessions/{{classSessionId}}/attendance/results?page=0&size=20&sort=student.studentCode,asc
Authorization: Bearer {{lecturerToken}}
```

HTTP 200 returns `AttendanceResultsResponse` in `data`:

- `session`: the same `AttendanceSessionResponse`, with effective OPEN/CLOSED status.
- `statistics`: `recorded`, `present`, `late`, `absent` count records across the
  entire requested session, independent of page/sort. `unmarked` counts distinct
  currently ACTIVE enrollees with no record in this session, even after expiry.
- `attendance`: `ResponsePage` with `pageNo`, `pageSize`, `totalPage`, and `items`.
  Each item has `id`, `studentId`, `studentCode`, `fullName`, `status`,
  `recognizedAt`, and `method`. No User entities or account details are exposed.

The list contains existing attendance records only; it does not synthesize
ABSENT records during GET. Historical records remain visible if enrollment is
later removed or deactivated. Because the existing schema has no roster snapshot,
`unmarked` reflects current enrollments, not a historical enrollment count.
Before explicit closing, an expired session can therefore report CLOSED with
unmarked students; call `/close` to persist the missing absences.
An opened session without records returns an empty page and zero recorded counts.
An unopened/nonexistent session returns 404.

Defaults are page 0, size 20, sort `student.studentCode,asc`. Supported sort fields
are `id`, `student.id`, `student.studentCode`, `student.fullName`, `status`,
`recognizedAt`, `method`, `createdAt`, `updatedAt`; all other fields return 400.
The results scope always comes from the path and authenticated lecturer.
`POST /api/sessions/{id}/attendance/recognize` is intentionally not implemented.

### Login and Legacy Manual Recording

Login body (use Postman variables with your existing account):

```json
{"username":"{{username_or_email}}","password":"{{password}}"}
```

Success is HTTP 200 with `status`, `message` and `data`. Login data contains
`accessToken`, `tokenType` (Bearer), `expiresIn` (seconds), and safe `user` fields:
`userId`, `username`, `email`, `roles`, `status`.

Body for the preserved `POST /api/lecturer/classes/{classId}/attendance` endpoint:

```json
{"attendanceSessionId":123,"studentId":456,"status":"PRESENT"}
```

Replace the example IDs with existing records. Status is PRESENT/LATE/ABSENT.
Success is HTTP 201 with AttendanceResponse; repeated submissions return 409.
Foreign session or unenrolled student returns 404. Closed session returns 409.
An unassigned lecturer receives 403 before a write occurs.

Errors consistently use the existing response envelope, without stack traces:

```json
{"status":403,"message":"Access denied"}
```

400 means invalid input; 401 means authentication is absent/invalid; 403 means
the authenticated caller lacks permission; 404 means a permitted resource is
missing; 409 means a business/database conflict. Unexpected server errors return
a generic 500. The old subject/semester/class controller catch-all blocks no
longer hide failures behind HTTP 200. Existing successful response shapes remain.

## 5. Request Flow

1. Login DTO validation runs, then AuthenticationManager loads the matching User
   and verifies password and active status. Successful legacy hashes are upgraded.
2. AuthService signs a short-lived token. No session or authentication cookie is
   created; the client holds the access token.
3. For later calls, send `Authorization: Bearer <accessToken>`.
4. Resource Server validates the JWT and the DB-backed converter creates a
   JwtAuthenticationToken with verified authorities. Its name is the canonical
   username, preserving the existing Principal-based enrolled-class API.
5. URL authorization runs, then controller/service `@PreAuthorize` checks role
   and ownership. Repositories query only the authorized student's/lecturer's
   scope; DTOs prevent password/entity-graph disclosure.

## 6. Configuration and Running

Set variables in IntelliJ's Run Configuration, your shell or deployment secret
manager. For local development, `application.properties` explicitly imports the
optional `.env` and `.env.local` files from the working directory using Spring
Boot Config Data, with `.env.local` taking precedence over `.env`.
Run from the `backend` directory (also set this as IntelliJ's working directory).
OS/IDE environment variables override both files; `.env.example` is not loaded.
The imported file uses Java properties syntax: plain `KEY=value` lines without
shell quotes or `export`, with backslashes escaped as required by that format.

| Variable | Required/default |
| --- | --- |
| `DB_URL` | Required: JDBC MySQL URL of the existing database |
| `DB_USERNAME` | Required: database login |
| `DB_PASSWORD` | Required: database password |
| `JWT_SECRET` | Required: Base64 of at least 32 random bytes |
| `JWT_ISSUER` | `oop-ptit` |
| `JWT_AUDIENCE` | `oop-ptit-api` |
| `JWT_TTL` | `15m`; allowed range 1 second to 1 hour |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173`; comma-separated explicit origins |
| `API_DOCS_ENABLED` | `false`; enables public Swagger only when explicitly true |
| `JPA_DDL_AUTO` | `validate` |
| `JPA_SHOW_SQL` | `false` |

For the supplied Docker Compose setup, MySQL is published at `localhost:3307`,
not `localhost:3306`. Preserve the existing `MYSQL_DATABASE` and
`MYSQL_ROOT_PASSWORD` in `.env`, then add the application settings to `.env.local`:

```properties
DB_URL=jdbc:mysql://localhost:3307/${MYSQL_DATABASE}
DB_USERNAME=root
DB_PASSWORD=${MYSQL_ROOT_PASSWORD}
```

This root login is for the existing local Docker setup only. Use a dedicated,
least-privileged account and an explicit JDBC URL in deployed environments.
Changing `MYSQL_ROOT_PASSWORD` does not reset the password in an existing Docker
volume. Do not delete that volume to resolve a connection/configuration error.

Generate a development key once, without displaying it:

```sh
export JWT_SECRET="$(openssl rand -base64 32)"
```

Keep a stable key in a secret manager per deployed environment. Do not run this
generation command on every application restart; replacing the key invalidates
all existing tokens. Never commit keys, passwords or populated Postman secrets.
Any database credentials previously committed to Git should be rotated; removing
them from the current file does not remove them from Git history.
For IntelliJ runs, configure the same `JWT_SECRET` in the run configuration or
the ignored `.env.local` file; a terminal's exported variables are not inherited
by an IDE that is already running. The legacy `.env` is already tracked by Git,
so ignore rules do not protect it: do not add new secrets there. Coordinate
credential rotation and removing that legacy file from version control separately.

After configuring the variables:

```sh
mvn spring-boot:run
```

Schema validation does not modify tables. Do not use create/create-drop on the
real database. If an existing schema does not validate, investigate the mismatch
and use a separately reviewed migration; do not discard data to make startup pass.
Tests alone use create-drop on isolated H2 databases with test-only random keys.

If startup reports `Unable to determine Dialect without JDBC metadata`, inspect
the earlier connection error. In particular, `not accept jdbcUrl, ${DB_URL}`
means the variable was not loaded. Check the working directory, `.env.local` and
run configuration before adding a dialect. A configured dialect cannot repair a
missing JDBC URL, incorrect password or unreachable database.

Use an existing ACTIVE admin account. If none exists, bootstrap one through a
trusted operator-controlled database/provisioning process using a BCrypt hash
generated by the configured PasswordEncoder. No default credentials or public
admin registration are provided. Plaintext passwords are deliberately rejected.

## 7. Testing and Postman

Run all tests, without a live DB or production secret:

```sh
mvn test
mvn -Dtest=JwtSecurityIntegrationTests,SecurityConfigTests test
```

Surefire explicitly attaches the managed Mockito Java agent for recent JDKs.
The security integration suite uses the real filter chain, real signed JWTs,
repositories and H2. It covers username/email login, wrong passwords, missing
tokens, expiry, signatures, missing/invalid claims, all three roles, direct
service authorization, class ownership, foreign sessions, student data isolation,
duplicate attendance, attendance session opening/status, concurrent opens,
manual recording/corrections, close/absence finalization, duplicate enrollments,
paged results with whole-session statistics, concurrent manual/close requests,
closed/expired sessions, reassignment, disabled/deleted users, changed roles, legacy password
upgrades, CORS, statelessness and error envelopes. Configuration tests reject
weak/missing secrets, unsafe token lifetimes and wildcard CORS origins.

Postman:

1. Set `baseUrl` to the application's URL (normally `http://localhost:8080`),
   and set private environment values `username_or_email` and `password` for an
   existing account. On Login, choose **No Auth** so an expired collection token
   is not sent accidentally.
2. POST `{{baseUrl}}/api/auth/login` with the JSON above. Store the token locally
   using this post-response script:

```javascript
pm.test("Login succeeds", () => pm.response.to.have.status(200));
if (pm.response.code === 200) {
  pm.environment.set("accessToken", pm.response.json().data.accessToken);
}
```

3. For authenticated requests choose **Bearer Token** with `{{accessToken}}`.
   GET `/api/auth/me` to verify identity. Repeat login using each role's account.
4. Check ADMIN -> `/api/admin/users` returns 200, STUDENT -> that same URL returns
   403, LECTURER -> `/api/student/profile` returns 403, and a request without
   Bearer authentication returns 401.
5. As STUDENT, GET `/api/student/profile`, `/api/student/attendance` and
   `/api/students/me/classes`. Adding another student's ID to query parameters
   must not change the caller's identity. `/api/admin/students/{otherId}` is 403.
6. As LECTURER, GET `/api/lecturer/classes` and the assigned class's sessions.
   Pick an existing class session that has not ended and has no attendance session.
   POST `/api/sessions/{id}/attendance/open` with `{"lateAfterMinutes":15}` (201),
   GET `/api/sessions/{id}/attendance` (200/OPEN), then repeat the open (409).
   POST `/api/sessions/{id}/attendance/manual` with `{"studentId":456,"status":"PRESENT"}`
   (201), repeat it (409), then POST `/api/sessions/{id}/attendance/close` (200).
   GET `/api/sessions/{id}/attendance/results` and verify missing ACTIVE students
   became ABSENT. PATCH `/api/attendance/{attendanceId}` with `{"status":"LATE"}`
   (200) and verify statistics change. Repeat close and verify no extra records.
   Try an unassigned class or an ADMIN/STUDENT token on all six endpoints (403).
   Keep the DB unchanged after rejected writes.
7. In a development environment only, use `JWT_TTL=2s`, restart, obtain a fresh
   token and wait more than two seconds: the next protected request is 401.
   Changing the signature or using a token signed with a different key is 401.
   Automated tests cover these cases without modifying production settings.

For Swagger, enable `API_DOCS_ENABLED=true` in development, open
`/swagger-ui/index.html`, call Login, and enter the token in **Authorize**. The
OpenAPI document declares the Bearer scheme; login has no security requirement.

## Deployment Notes

- Use HTTPS and login rate limiting at the trusted reverse proxy/API gateway.
  This change does not introduce a distributed lockout/rate-limit subsystem.
- This is access-token authentication, not a full OAuth authorization server.
  No refresh token or per-token logout/revocation store is implemented. Client
  logout discards its token; account deactivation or key rotation invalidates
  access. Otherwise tokens remain usable until expiry, including after a
  password-only change. Add a reviewed token-version/revocation design if needed.
- Database-backed authority checks intentionally perform one account lookup per
  authenticated request. Avoid caching indefinitely, which would delay revocation.
- New resource APIs must add service-level role/ownership checks, not just rely
  on authenticated URL access. Keep the role enum's stored values stable.
- MySQL-specific schema/locking behavior should also be verified in staging;
  the automated local suite does not connect to the production database.
