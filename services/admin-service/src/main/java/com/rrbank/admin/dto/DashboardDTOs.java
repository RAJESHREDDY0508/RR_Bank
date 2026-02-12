package com.rrbank.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class DashboardDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardStatsResponse {
        // Customer Stats
        private long totalCustomers;
        private long activeCustomers;
        private long newCustomersToday;
        private double customerGrowthPercent;

        // Account Stats
        private long totalAccounts;
        private long activeAccounts;
        private long frozenAccounts;

        // Financial Stats
        private BigDecimal totalBalance;
        private BigDecimal todayDeposits;
        private BigDecimal todayWithdrawals;
        private BigDecimal todayTransfers;

        // Transaction Stats
        private long totalTransactions;
        private long todayTransactions;
        private double transactionGrowthPercent;

        // Alerts
        private long pendingFraudAlerts;
        private long pendingAccountRequests;
        private long pendingKycReviews;

        // Admin Stats
        private long activeAdmins;
        private long adminActionsToday;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartDataResponse {
        private List<String> labels;
        private List<ChartDataset> datasets;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartDataset {
        private String label;
        private List<Number> data;
        private String borderColor;
        private String backgroundColor;
        private Boolean fill;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionChartData {
        private List<String> dates;
        private List<Long> deposits;
        private List<Long> withdrawals;
        private List<Long> transfers;
        private List<BigDecimal> amounts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerGrowthData {
        private List<String> dates;
        private List<Long> newCustomers;
        private List<Long> cumulativeCustomers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivityItem {
        private String id;
        private String type;
        private String description;
        private String entityType;
        private String entityId;
        private String status;
        private String performedBy;
        private LocalDateTime timestamp;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickStats {
        private String label;
        private String value;
        private String change;
        private String changeType; // positive, negative, neutral
        private String icon;
    }
}
