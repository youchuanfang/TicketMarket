package com.ticketmarket.controller;

import com.ticketmarket.common.ApiException;
import com.ticketmarket.common.Result;
import com.ticketmarket.config.AuthContext;
import com.ticketmarket.service.Phase4TicketFlowService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class Phase5OperationsController {
    private final Phase4TicketFlowService service;

    public Phase5OperationsController(Phase4TicketFlowService service) {
        this.service = service;
    }

    @PostMapping("/api/user/orders/{orderId}/refund")
    public Result<Map<String, Object>> applyRefund(@PathVariable Long orderId) {
        return Result.ok(service.applyRefund(orderId, currentUserId()));
    }

    @GetMapping("/api/user/refunds")
    public Result<List<Map<String, Object>>> userRefunds() {
        return Result.ok(service.userRefunds(currentUserId()));
    }

    @GetMapping("/api/admin/refunds")
    public Result<List<Map<String, Object>>> adminRefunds() {
        return Result.ok(service.adminRefunds());
    }

    @PostMapping("/api/admin/refunds/{id}/approve")
    public Result<Map<String, Object>> approveRefund(@PathVariable Long id) {
        return Result.ok(service.approveRefund(id));
    }

    @PostMapping("/api/admin/refunds/{id}/reject")
    public Result<Map<String, Object>> rejectRefund(@PathVariable Long id) {
        return Result.ok(service.rejectRefund(id));
    }

    @PostMapping("/api/checker/tickets/verify")
    public Result<Map<String, Object>> verifyTicket(@RequestBody Map<String, Object> payload) {
        return Result.ok(service.verifyTicket(payload, currentUserId()));
    }

    @GetMapping("/api/checker/checkins")
    public Result<List<Map<String, Object>>> checkerCheckins() {
        return Result.ok(service.checkins());
    }

    @GetMapping("/api/admin/checkins")
    public Result<List<Map<String, Object>>> adminCheckins() {
        return Result.ok(service.checkins());
    }

    @GetMapping("/api/user/messages")
    public Result<List<Map<String, Object>>> userMessages() {
        return Result.ok(service.userMessages(currentUserId()));
    }

    @PostMapping("/api/user/messages/{id}/read")
    public Result<Map<String, Object>> readMessage(@PathVariable Long id) {
        return Result.ok(service.readMessage(id, currentUserId()));
    }

    @GetMapping("/api/admin/messages")
    public Result<List<Map<String, Object>>> adminMessages() {
        return Result.ok(service.adminMessages());
    }

    @PostMapping("/api/admin/announcements")
    public Result<Map<String, Object>> announcement(@RequestBody Map<String, Object> payload) {
        return Result.ok(service.announcement(payload));
    }

    @GetMapping("/api/admin/statistics/overview")
    public Result<Map<String, Object>> statisticsOverview() {
        return Result.ok(service.statisticsOverview());
    }

    @GetMapping("/api/admin/statistics/sales")
    public Result<Map<String, Object>> statisticsSales() {
        return Result.ok(service.statisticsOverview());
    }

    @GetMapping("/api/admin/statistics/rush")
    public Result<Map<String, Object>> statisticsRush() {
        return Result.ok(service.statisticsOverview());
    }

    @GetMapping("/api/admin/statistics/checkin")
    public Result<Map<String, Object>> statisticsCheckin() {
        return Result.ok(service.statisticsOverview());
    }

    @GetMapping("/api/admin/statistics/refund")
    public Result<Map<String, Object>> statisticsRefund() {
        return Result.ok(service.statisticsOverview());
    }

    @GetMapping("/api/admin/operation-logs")
    public Result<List<Map<String, Object>>> operationLogs() {
        return Result.ok(service.operationLogs());
    }

    @GetMapping("/api/admin/risk-logs")
    public Result<List<Map<String, Object>>> riskLogs() {
        return Result.ok(service.riskLogs());
    }

    @GetMapping("/api/admin/blacklist")
    public Result<List<Map<String, Object>>> blacklist() {
        return Result.ok(service.blacklist());
    }

    @PostMapping("/api/admin/blacklist")
    public Result<Map<String, Object>> addBlacklist(@RequestBody Map<String, Object> payload) {
        return Result.ok(service.addBlacklist(payload));
    }

    @DeleteMapping("/api/admin/blacklist/{id}")
    public Result<Void> removeBlacklist(@PathVariable Long id) {
        service.removeBlacklist(id);
        return Result.ok();
    }

    private static Long currentUserId() {
        Long userId = AuthContext.currentUserId();
        if (userId == null) {
            throw new ApiException(401, "请先登录");
        }
        return userId;
    }
}
