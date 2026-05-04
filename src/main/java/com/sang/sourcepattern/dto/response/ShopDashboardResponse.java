package com.sang.sourcepattern.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShopDashboardResponse {
    // KPI Stats
    BigDecimal totalRevenue;
    BigDecimal revenueThisMonth;
    long totalBookings;
    long pendingBookings;
    long totalCustomers;
    long totalPets;

    // Charts
    List<RevenueChartData> revenueChart;
    List<ServiceStat> topServices;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueChartData {
        String date;
        BigDecimal amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceStat {
        String name;
        long count;
    }
}
