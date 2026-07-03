package com.ticketmarket.controller;

import com.ticketmarket.common.Result;
import com.ticketmarket.service.Phase3ResourceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portal")
public class Phase3PortalController {
    private final Phase3ResourceService service;

    public Phase3PortalController(Phase3ResourceService service) {
        this.service = service;
    }

    @GetMapping("/venues/hot")
    public Result<List<Map<String, Object>>> hotVenues() {
        return Result.ok(service.hotVenues());
    }

    @GetMapping("/performances/{performanceId}/sessions")
    public Result<List<Map<String, Object>>> performanceSessions(@PathVariable Long performanceId) {
        return Result.ok(service.sessionsByPerformance(performanceId));
    }

    @GetMapping("/sessions/{sessionId}/ticket-levels")
    public Result<List<Map<String, Object>>> ticketLevels(@PathVariable Long sessionId) {
        return Result.ok(service.ticketLevels(sessionId));
    }

    @GetMapping("/sessions/{sessionId}/seats")
    public Result<List<Map<String, Object>>> sessionSeats(@PathVariable Long sessionId) {
        return Result.ok(service.sessionSeats(sessionId));
    }

    @GetMapping("/sessions/{sessionId}/active-batch")
    public Result<Map<String, Object>> activeBatch(@PathVariable Long sessionId) {
        return Result.ok(service.activeBatch(sessionId));
    }
}
