package com.ticketmarket.controller;

import com.ticketmarket.common.Result;
import com.ticketmarket.service.DemoDataService;
import com.ticketmarket.service.PersistentPerformanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final DemoDataService dataService;
    private final PersistentPerformanceService performanceService;

    public AdminController(DemoDataService dataService, PersistentPerformanceService performanceService) {
        this.dataService = dataService;
        this.performanceService = performanceService;
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.ok(Map.of(
                "performanceCount", performanceService.adminPerformances().size(),
                "movieCount", dataService.movies().size(),
                "orderCount", 18,
                "ticketCount", 26,
                "refundPending", 3,
                "checkinToday", 42
        ));
    }
}
