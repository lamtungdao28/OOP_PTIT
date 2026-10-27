export type Role = 'ADMIN' | 'LECTURER' | 'STUDENT'
export type AccountStatus = 'ACTIVE' | 'INACTIVE' | 'LOCKED'
export type AttendanceStatus = 'PRESENT' | 'LATE' | 'ABSENT'
export type SessionStatus = 'OPEN' | 'CLOSED'

export interface ApiResponse<T> { status: number; message: string; data: T }
export interface PageResponse<T> { pageNo: number; pageSize: number; totalPage: number; items: T }
export interface CurrentUser { userId: number; username: string; email: string; roles: Role[]; status: AccountStatus }
export interface LoginResponse { accessToken: string; tokenType: string; expiresIn: number; user: CurrentUser }
export interface Student { id: number; studentCode: string; fullName: string; dateOfBirth: string; phone: string; username?: string; email?: string; status?: AccountStatus }
export interface Lecturer { id: number; lecturerCode: string; fullName: string; department: string; phone: string; username: string; email: string; status: AccountStatus }
export interface Subject { id: number; subjectCode: string; subjectName: string; credits: number; status: AccountStatus; createdAt?: string; updatedAt?: string }
export interface Semester { id: number; name: string; createdAt?: string; updatedAt?: string }
export interface CourseClass { id: number; classCode: string; maxStudents: number; status: AccountStatus; subjectId: number; semesterId: number; lecturerId: number; createdAt?: string; updatedAt?: string }
export interface StudentEnrollment { student: Student; courseClass: CourseClass }
export interface ClassSession { id: number; classId: number; sessionNo: number; sessionDate: string; startTime: string; endTime: string; createdAt: string; updatedAt: string }
export interface AttendanceSession { id: number; classSessionId: number; classId: number; openedAt: string; closedAt: string | null; lateAfterMinutes: number; status: SessionStatus }
export interface Attendance { id: number; classId: number; attendanceSessionId: number; studentId: number; studentCode?: string; fullName?: string; status: AttendanceStatus; recognizedAt: string; method: number }
export interface AttendanceStats { recorded: number; present: number; late: number; absent: number; unmarked: number }
export interface AttendanceResults { session: AttendanceSession; statistics: AttendanceStats; attendance: PageResponse<Attendance[]> }
export interface FaceProfile { registered: boolean; status: AccountStatus | null; modelName: string | null; updatedAt: string | null }
