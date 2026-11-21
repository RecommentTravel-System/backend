package org.example.wayveesystem.service;

import jakarta.servlet.http.HttpServletRequest;
import org.example.wayveesystem.dto.request.AccessLogRecordRequest;
import org.example.wayveesystem.dto.response.AccessLogResponse;
import org.example.wayveesystem.dto.response.TrafficStatsResponse;
import org.springframework.data.domain.Page;

public interface AccessLogService {

    AccessLogResponse recordAccess(AccessLogRecordRequest request, HttpServletRequest httpRequest);

    Page<AccessLogResponse> getAccessLogs(int page, int size, String keyword, String deviceType, String startDate, String endDate);

    TrafficStatsResponse getTrafficStats(String period);

    void deleteLog(Long id);

    void clearOldLogs(int daysToKeep);
}
