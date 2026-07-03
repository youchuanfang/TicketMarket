package com.ticketmarket.controller;

import com.ticketmarket.common.Result;
import com.ticketmarket.service.Phase3ResourceService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class Phase3AdminController {
    private final Phase3ResourceService service;

    public Phase3AdminController(Phase3ResourceService service) {
        this.service = service;
    }

    @GetMapping("/venues")
    public Result<List<Map<String, Object>>> venues() {
        return Result.ok(service.venues());
    }

    @GetMapping("/venues/{id}")
    public Result<Map<String, Object>> venue(@PathVariable Long id) {
        return Result.ok(service.venue(id));
    }

    @PostMapping("/venues")
    public Result<Map<String, Object>> createVenue(@RequestBody Map<String, Object> payload) {
        return Result.ok(service.createVenue(payload));
    }

    @PutMapping("/venues/{id}")
    public Result<Map<String, Object>> updateVenue(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return Result.ok(service.updateVenue(id, payload));
    }

    @DeleteMapping("/venues/{id}")
    public Result<Void> deleteVenue(@PathVariable Long id) {
        service.deleteVenue(id);
        return Result.ok();
    }

    @GetMapping("/venues/{venueId}/areas")
    public Result<List<Map<String, Object>>> areas(@PathVariable Long venueId) {
        return Result.ok(service.areas(venueId));
    }

    @PostMapping("/venues/{venueId}/areas")
    public Result<Map<String, Object>> createArea(@PathVariable Long venueId, @RequestBody Map<String, Object> payload) {
        return Result.ok(service.createArea(venueId, payload));
    }

    @PutMapping("/venue-areas/{id}")
    public Result<Map<String, Object>> updateArea(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return Result.ok(service.updateArea(id, payload));
    }

    @DeleteMapping("/venue-areas/{id}")
    public Result<Void> deleteArea(@PathVariable Long id) {
        service.deleteArea(id);
        return Result.ok();
    }

    @GetMapping("/venues/{venueId}/seats")
    public Result<List<Map<String, Object>>> seats(@PathVariable Long venueId) {
        return Result.ok(service.seats(venueId));
    }

    @PostMapping("/venues/{venueId}/seats/generate")
    public Result<List<Map<String, Object>>> generateSeats(@PathVariable Long venueId, @RequestBody Map<String, Object> payload) {
        return Result.ok(service.generateSeats(venueId, payload));
    }

    @PutMapping("/seats/{id}")
    public Result<Map<String, Object>> updateSeat(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return Result.ok(service.updateSeat(id, payload));
    }

    @PutMapping("/seats/batch-disable")
    public Result<Void> batchDisable(@RequestBody Map<String, List<Long>> payload) {
        service.batchSeatDisabled(payload.getOrDefault("ids", List.of()), true);
        return Result.ok();
    }

    @PutMapping("/seats/batch-enable")
    public Result<Void> batchEnable(@RequestBody Map<String, List<Long>> payload) {
        service.batchSeatDisabled(payload.getOrDefault("ids", List.of()), false);
        return Result.ok();
    }

    @GetMapping("/sessions")
    public Result<List<Map<String, Object>>> sessions() {
        return Result.ok(service.sessions());
    }

    @PostMapping("/sessions")
    public Result<Map<String, Object>> createSession(@RequestBody Map<String, Object> payload) {
        return Result.ok(service.createSession(payload));
    }

    @PutMapping("/sessions/{id}")
    public Result<Map<String, Object>> updateSession(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return Result.ok(service.updateSession(id, payload));
    }

    @DeleteMapping("/sessions/{id}")
    public Result<Void> deleteSession(@PathVariable Long id) {
        service.deleteSession(id);
        return Result.ok();
    }

    @GetMapping("/sessions/{sessionId}/seats")
    public Result<List<Map<String, Object>>> sessionSeats(@PathVariable Long sessionId) {
        return Result.ok(service.sessionSeats(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/init-seats")
    public Result<List<Map<String, Object>>> initSessionSeats(@PathVariable Long sessionId) {
        return Result.ok(service.initSessionSeats(sessionId));
    }

    @PutMapping("/session-seats/batch-status")
    public Result<Void> updateSessionSeatStatus(@RequestBody Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        List<Number> numbers = (List<Number>) payload.getOrDefault("ids", List.of());
        List<Long> ids = numbers.stream().map(Number::longValue).toList();
        service.updateSessionSeatStatus(ids, String.valueOf(payload.getOrDefault("status", "AVAILABLE")));
        return Result.ok();
    }

    @GetMapping("/sessions/{sessionId}/ticket-levels")
    public Result<List<Map<String, Object>>> sessionTicketLevels(@PathVariable Long sessionId) {
        return Result.ok(service.ticketLevels(sessionId));
    }

    @PostMapping("/ticket-levels")
    public Result<Map<String, Object>> createTicketLevel(@RequestBody Map<String, Object> payload) {
        return Result.ok(service.createTicketLevel(payload));
    }

    @PutMapping("/ticket-levels/{id}")
    public Result<Map<String, Object>> updateTicketLevel(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return Result.ok(service.updateTicketLevel(id, payload));
    }

    @DeleteMapping("/ticket-levels/{id}")
    public Result<Void> deleteTicketLevel(@PathVariable Long id) {
        service.deleteTicketLevel(id);
        return Result.ok();
    }

    @GetMapping("/sale-batches")
    public Result<List<Map<String, Object>>> saleBatches() {
        return Result.ok(service.saleBatches());
    }

    @GetMapping("/sale-batches/{id}")
    public Result<Map<String, Object>> saleBatch(@PathVariable Long id) {
        return Result.ok(service.saleBatch(id));
    }

    @PostMapping("/sale-batches")
    public Result<Map<String, Object>> createSaleBatch(@RequestBody Map<String, Object> payload) {
        return Result.ok(service.createSaleBatch(payload));
    }

    @PutMapping("/sale-batches/{id}")
    public Result<Map<String, Object>> updateSaleBatch(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return Result.ok(service.updateSaleBatch(id, payload));
    }

    @PostMapping("/sale-batches/{id}/start")
    public Result<Map<String, Object>> startSaleBatch(@PathVariable Long id) {
        return Result.ok(service.changeBatchStatus(id, "SELLING"));
    }

    @PostMapping("/sale-batches/{id}/lock")
    public Result<Map<String, Object>> lockSaleBatch(@PathVariable Long id) {
        return Result.ok(service.changeBatchStatus(id, "LOCKED"));
    }

    @PostMapping("/sale-batches/{id}/init-redis-stock")
    public Result<Map<String, Object>> initRedisStock(@PathVariable Long id) {
        return Result.ok(service.initRedisStock(id));
    }

    @GetMapping("/sale-batches/{id}/stock-summary")
    public Result<Map<String, Object>> stockSummary(@PathVariable Long id) {
        return Result.ok(service.batchStockSummary(id));
    }

    @GetMapping("/stock-pool")
    public Result<List<Map<String, Object>>> stockPool() {
        return Result.ok(service.stockPool());
    }

    @GetMapping("/sessions/{sessionId}/stock-pool")
    public Result<List<Map<String, Object>>> sessionStockPool(@PathVariable Long sessionId) {
        return Result.ok(service.stockPoolBySession(sessionId));
    }

    @PostMapping("/stock-pool/add")
    public Result<Map<String, Object>> addStockPool(@RequestBody Map<String, Object> payload) {
        return Result.ok(service.addStockPool(payload));
    }

    @PostMapping("/stock-pool/release-to-batch")
    public Result<Map<String, Object>> releaseToBatch(@RequestBody Map<String, Object> payload) {
        return Result.ok(service.releaseStockToBatch(payload));
    }
}
