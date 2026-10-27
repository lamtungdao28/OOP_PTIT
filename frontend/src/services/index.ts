import { apiClient } from './apiClient'
import type { ApiResponse, Attendance, AttendanceResults, AttendanceSession, ClassSession, CourseClass, CurrentUser, FaceProfile, Lecturer, LoginResponse, PageResponse, Semester, Student, StudentEnrollment, Subject } from '@/types/api'

const data = <T>(promise: Promise<{ data: ApiResponse<T> }>) => promise.then((r) => r.data.data)
const pageParams = (page = 0, size = 20) => ({ page, size })

export const authService = {
  login: (payload: { username: string; password: string }) => data<LoginResponse>(apiClient.post('/api/auth/login', payload)),
  me: () => data<CurrentUser>(apiClient.get('/api/auth/me')),
}

export const studentService = {
  list: (page = 0, size = 20, filter?: string) => data<PageResponse<Student[]>>(apiClient.get('/api/admin/students', { params: { ...pageParams(page, size), ...(filter ? { student: filter } : {}) } })),
  detail: (id: number) => data<Student>(apiClient.get(`/api/admin/students/${id}`)),
  create: (payload: unknown) => data<Student>(apiClient.post('/api/admin/students', payload)),
  update: (id: number, payload: unknown) => data<Student>(apiClient.patch(`/api/admin/students/${id}`, payload)),
  status: (id: number, status: string) => data<Student>(apiClient.patch(`/api/admin/students/${id}/status`, { status })),
  profile: () => data<Student>(apiClient.get('/api/student/profile')),
  myClasses: (page = 0) => data<PageResponse<CourseClass[]>>(apiClient.get('/api/students/me/classes', { params: pageParams(page) })),
  attendance: (page = 0) => data<PageResponse<Attendance[]>>(apiClient.get('/api/student/attendance', { params: pageParams(page) })),
}

export const lecturerService = {
  list: (page = 0, size = 20, filter?: string) => data<PageResponse<Lecturer[]>>(apiClient.get('/api/admin/lecturers', { params: { ...pageParams(page, size), ...(filter ? { lecturer: filter } : {}) } })),
  create: (payload: unknown) => data<Lecturer>(apiClient.post('/api/admin/lecturers', payload)),
  update: (id: number, payload: unknown) => data<Lecturer>(apiClient.patch(`/api/admin/lecturers/${id}`, payload)),
}

export const subjectService = {
  list: (page = 0, size = 20, filter?: string) => data<PageResponse<Subject[]>>(apiClient.get('/api/admin/subjects', { params: { ...pageParams(page, size), ...(filter ? { subject: filter } : {}) } })),
  create: (payload: unknown) => data<number>(apiClient.post('/api/admin/subjects', payload)),
  update: (id: number, payload: unknown) => apiClient.patch(`/api/admin/subjects/${id}`, payload).then(() => undefined),
}

export const semesterService = {
  list: (page = 0, size = 20) => data<PageResponse<Semester[]>>(apiClient.get('/api/admin/semesters', { params: pageParams(page, size) })),
  create: (payload: { name: string }) => data<number>(apiClient.post('/api/admin/semesters', payload)),
}

export const classService = {
  list: (page = 0, size = 20, filter?: string) => data<PageResponse<CourseClass[]>>(apiClient.get('/api/admin/classes', { params: { ...pageParams(page, size), ...(filter ? { courseClass: filter } : {}) } })),
  lecturerClasses: (page = 0) => data<PageResponse<CourseClass[]>>(apiClient.get('/api/lecturers/me/classes', { params: pageParams(page) })),
  create: (payload: unknown) => data<number>(apiClient.post('/api/admin/classes', payload)),
  update: (id: number, payload: unknown) => data<CourseClass>(apiClient.patch(`/api/admin/classes/${id}`, payload)),
  assignLecturer: (id: number, lecturerId: number) => data<CourseClass>(apiClient.patch(`/api/admin/classes/${id}/lecturer`, { lecturerId })),
  students: (id: number, page = 0) => data<PageResponse<StudentEnrollment[]>>(apiClient.get(`/api/classes/${id}/students`, { params: pageParams(page) }))
    .then(response => ({ ...response, items: response.items.map(enrollment => enrollment.student) })),
  enroll: (id: number, studentId: number) => data<number>(apiClient.post(`/api/admin/classes/${id}/enrollments`, { studentId })),
  removeStudent: (id: number, studentId: number) => apiClient.delete(`/api/admin/classes/${id}/enrollments/${studentId}`),
  sessions: (id: number, page = 0) => data<PageResponse<ClassSession[]>>(apiClient.get(`/api/classes/${id}/sessions`, { params: pageParams(page) })),
  createSession: (id: number, payload: unknown) => data<ClassSession>(apiClient.post(`/api/classes/${id}/sessions`, payload)),
}

export const attendanceService = {
  open: (sessionId: number, lateAfterMinutes: number) => data<AttendanceSession>(apiClient.post(`/api/sessions/${sessionId}/attendance/open`, { lateAfterMinutes })),
  get: (sessionId: number) => data<AttendanceSession>(apiClient.get(`/api/sessions/${sessionId}/attendance`)),
  results: (sessionId: number, page = 0) => data<AttendanceResults>(apiClient.get(`/api/sessions/${sessionId}/attendance/results`, { params: pageParams(page) })),
  manual: (sessionId: number, studentId: number, status: string) => data<Attendance>(apiClient.post(`/api/sessions/${sessionId}/attendance/manual`, { studentId, status })),
  update: (attendanceId: number, status: string) => data<Attendance>(apiClient.patch(`/api/attendance/${attendanceId}`, { status })),
  recognize: (sessionId: number, image: Blob) => { const form = new FormData(); form.append('image', image, 'capture.jpg'); return data<Attendance>(apiClient.post(`/api/sessions/${sessionId}/attendance/recognize`, form)) },
  close: (sessionId: number) => data<AttendanceSession>(apiClient.post(`/api/sessions/${sessionId}/attendance/close`)),
}

export const faceService = {
  get: () => data<FaceProfile>(apiClient.get('/api/students/me/face-profile')),
  save: (image: Blob, replace = false) => { const form = new FormData(); form.append('image', image, 'face.jpg'); return data<FaceProfile>((replace ? apiClient.put : apiClient.post)('/api/students/me/face-profile', form)) },
}
