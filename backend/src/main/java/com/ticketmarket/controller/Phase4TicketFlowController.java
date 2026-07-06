package com.ticketmarket.controller;

import com.ticketmarket.common.ApiException;
import com.ticketmarket.common.Result;
import com.ticketmarket.config.AuthContext;
import com.ticketmarket.service.Phase4TicketFlowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class Phase4TicketFlowController {
    private final Phase4TicketFlowService service;

    public Phase4TicketFlowController(Phase4TicketFlowService service) {
        this.service = service;
    }

    @PostMapping("/api/rush/submit")
    public Result<Map<String, Object>> submitRush(@RequestBody Map<String, Object> payload) {
        return Result.ok(service.submitRush(currentUserId(), payload));
    }

    @GetMapping("/api/rush/{requestId}")
    public Result<Map<String, Object>> rushRequest(@PathVariable String requestId) {
        return Result.ok(service.rushRequest(requestId, currentUserId()));
    }

    @GetMapping("/api/rush/{requestId}/result")
    public Result<Map<String, Object>> rushResult(@PathVariable String requestId) {
        return Result.ok(service.rushResult(requestId, currentUserId()));
    }

    @PostMapping("/api/seats/lock")
    public Result<Map<String, Object>> lockSeats(@RequestBody Map<String, Object> payload) {
        return Result.ok(service.lockSeats(currentUserId(), payload));
    }

    @PostMapping("/api/seats/release")
    public Result<Map<String, Object>> releaseSeats(@RequestBody Map<String, Object> payload) {
        return Result.ok(service.releaseSeats(currentUserId(), payload));
    }

    @GetMapping("/api/seats/locks/{sessionId}")
    public Result<Map<String, Object>> locks(@PathVariable Long sessionId) {
        return Result.ok(service.locks(sessionId));
    }

    @PostMapping("/api/orders/create")
    public Result<Map<String, Object>> createOrder(@RequestBody Map<String, Object> payload) {
        return Result.ok(service.createOrder(currentUserId(), payload));
    }

    @PostMapping("/api/reservations")
    public Result<Map<String, Object>> createReservation(@RequestBody Map<String, Object> payload) {
        return Result.ok(service.createReservation(currentUserId(), payload));
    }

    @GetMapping("/api/reservations/latest")
    public Result<Map<String, Object>> latestReservation(@RequestParam(required = false) Long performanceId) {
        return Result.ok(service.latestReservation(currentUserId(), performanceId));
    }

    @PostMapping("/api/orders/{id}/cancel")
    public Result<Map<String, Object>> cancelOrder(@PathVariable Long id) {
        return Result.ok(service.cancelOrder(id, currentUserId()));
    }

    @PostMapping("/api/payment/{orderId}/pay")
    public Result<Map<String, Object>> pay(@PathVariable Long orderId, @RequestBody Map<String, Object> payload) {
        return Result.ok(service.pay(orderId, currentUserId(), payload));
    }

    @GetMapping("/api/payment/{orderId}")
    public Result<Map<String, Object>> payment(@PathVariable Long orderId) {
        return Result.ok(service.payment(orderId, currentUserId()));
    }

    @GetMapping("/api/user/orders")
    public Result<List<Map<String, Object>>> userOrders() {
        return Result.ok(service.userOrders(currentUserId()));
    }

    @GetMapping("/api/user/orders/{id}")
    public Result<Map<String, Object>> userOrder(@PathVariable Long id) {
        return Result.ok(service.order(id, currentUserId()));
    }

    @GetMapping("/api/user/tickets")
    public Result<List<Map<String, Object>>> userTickets() {
        return Result.ok(service.userTickets(currentUserId()));
    }

    @GetMapping("/api/user/tickets/{id}")
    public Result<Map<String, Object>> userTicket(@PathVariable Long id) {
        return Result.ok(service.ticket(id, currentUserId()));
    }

    @GetMapping("/api/admin/orders")
    public Result<List<Map<String, Object>>> adminOrders() {
        return Result.ok(service.adminOrders());
    }

    @GetMapping("/api/admin/tickets")
    public Result<List<Map<String, Object>>> adminTickets() {
        return Result.ok(service.adminTickets());
    }

    private static Long currentUserId() {
        Long userId = AuthContext.currentUserId();
        if (userId == null) {
            throw new ApiException(401, "请先登录");
        }
        return userId;
    }
}
