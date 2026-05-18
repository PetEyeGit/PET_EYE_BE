package com.sang.sourcepattern.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffChangeRequestResponse {
    int id;
    int bookingId;
    String reason;
    String status;
    StaffDto oldStaff;
    StaffDto proposedStaff;

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class StaffDto {
        int id;
        String fullName;
    }
}
