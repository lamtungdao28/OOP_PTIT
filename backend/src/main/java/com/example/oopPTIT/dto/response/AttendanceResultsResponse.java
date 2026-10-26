package com.example.oopPTIT.dto.response;

import java.util.List;

public record AttendanceResultsResponse(AttendanceSessionResponse session, AttendanceStatisticsResponse statistics,
                                        ResponsePage<List<AttendanceResultResponse>> attendance) {
}
