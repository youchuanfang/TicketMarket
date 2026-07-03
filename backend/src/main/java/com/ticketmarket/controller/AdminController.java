package com.ticketmarket.controller;

import com.ticketmarket.common.Result;
import com.ticketmarket.service.DemoDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final DemoDataService dataService;

    public AdminController(DemoDataService dataService) {
        this.dataService = dataService;
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.ok(Map.of(
                "performanceCount", dataService.performances().size(),
                "movieCount", dataService.movies().size(),
                "orderCount", 18,
                "ticketCount", 26,
                "refundPending", 3,
                "checkinToday", 42
        ));
    }
}
