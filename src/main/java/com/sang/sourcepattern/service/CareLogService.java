package com.sang.sourcepattern.service;

import com.sang.sourcepattern.dto.request.CareLogRequest;
import com.sang.sourcepattern.dto.response.CareLogResponse;
import java.util.List;

public interface CareLogService {
    CareLogResponse addLog(int bookingId, CareLogRequest request, String email);
    List<CareLogResponse> getLogsByBooking(int bookingId);
}
