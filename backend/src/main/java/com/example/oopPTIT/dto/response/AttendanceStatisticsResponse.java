package com.example.oopPTIT.dto.response;

public record AttendanceStatisticsResponse(long recorded, long present, long late, long absent, long unmarked) {
}
