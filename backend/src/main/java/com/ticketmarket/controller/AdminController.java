package com.ticketmarket.controller;

import com.ticketmarket.common.Result;
import com.ticketmarket.service.PersistentMovieService;
import com.ticketmarket.service.PersistentPerformanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final PersistentPerformanceService performanceService;
    private final PersistentMovieService movieService;

    public AdminController(PersistentPerformanceService performanceService, PersistentMovieService movieService) {
        this.performanceService = performanceService;
        this.movieService = movieService;
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.ok(Map.of(
                "performanceCount", performanceService.adminPerformances().size(),
                "movieCount", movieService.adminMovies().size(),
                "orderCount", 18,
                "ticketCount", 26,
                "refundPending", 3,
                "checkinToday", 42
        ));
    }
}
