package com.rrbank.admin.controller;

import com.rrbank.admin.dto.DashboardDTOs.*;
import com.rrbank.admin.dto.common.ApiResponse;
import com.rrbank.admin.entity.Permission;
import com.rrbank.admin.security.RequirePermission;
import com.rrbank.admin.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Admin dashboard statistics and charts")
@RequirePermission(Permission.DASHBOARD_READ)
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard statistics", description = "Get all KPIs for the admin dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats() {
        DashboardStatsResponse stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/charts/transactions")
    @Operation(summary = "Get transaction chart data", description = "Get transaction trends for charts")
    public ResponseEntity<ApiResponse<TransactionChartData>> getTransactionChartData(
            @RequestParam(defaultValue = "30") int days
    ) {
        TransactionChartData data = dashboardService.getTransactionChartData(days);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/charts/customers")
    @Operation(summary = "Get customer growth data", description = "Get customer growth trends for charts")
    public ResponseEntity<ApiResponse<CustomerGrowthData>> getCustomerGrowthData(
            @RequestParam(defaultValue = "30") int days
    ) {
        CustomerGrowthData data = dashboardService.getCustomerGrowthData(days);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/recent-activity")
    @Operation(summary = "Get recent activity", description = "Get recent admin activities")
    public ResponseEntity<ApiResponse<List<RecentActivityItem>>> getRecentActivity() {
        List<RecentActivityItem> activities = dashboardService.getRecentActivity();
        return ResponseEntity.ok(ApiResponse.success(activities));
    }
}
