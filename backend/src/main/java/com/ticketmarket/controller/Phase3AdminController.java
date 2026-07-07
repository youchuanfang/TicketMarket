package com.ticketmarket.controller;

import com.ticketmarket.common.Result;
import com.ticketmarket.model.MovieCard;
import com.ticketmarket.model.PerformanceCard;
import com.ticketmarket.service.PersistentMovieService;
import com.ticketmarket.service.PersistentPerformanceService;
import com.ticketmarket.service.Phase3ResourceService;
import com.ticketmarket.service.HomepageRecommendationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class Phase3AdminController {
    private final Phase3ResourceService service;
    private final PersistentPerformanceService performanceService;
    private final PersistentMovieService movieService;
    private final HomepageRecommendationService homepageRecommendationService;

    public Phase3AdminController(Phase3ResourceService service, PersistentPerformanceService performanceService, PersistentMovieService movieService, HomepageRecommendationService homepageRecommendationService) {
        this.service = service;
        this.performanceService = performanceService;
        this.movieService = movieService;
        this.homepageRecommendationService = homepageRecommendationService;
    }

    @GetMapping("/performances")
    public Result<List<PerformanceCard>> performances() {
        return Result.ok(performanceService.adminPerformances());
    }

    @GetMapping("/performances/{id}")
    public Result<PerformanceCard> performance(@PathVariable Long id) {
        return Result.ok(performanceService.adminPerformance(id));
    }

    @PostMapping("/performances")
    public Result<PerformanceCard> createPerformance(@RequestBody Map<String, Object> payload) {
        return Result.ok(performanceService.createPerformance(payload));
    }

    @PutMapping("/performances/{id}")
    public Result<PerformanceCard> updatePerformance(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return Result.ok(performanceService.updatePerformance(id, payload));
    }

    @PutMapping("/performances/{id}/publish")
    public Result<PerformanceCard> publishPerformance(@PathVariable Long id) {
        return Result.ok(performanceService.publish(id));
    }

    @PutMapping("/performances/{id}/offline")
    public Result<PerformanceCard> offlinePerformance(@PathVariable Long id) {
        return Result.ok(performanceService.offline(id));
    }

    @DeleteMapping("/performances/{id}")
    public Result<Void> deletePerformance(@PathVariable Long id) {
        performanceService.deletePerformance(id);
        return Result.ok();
    }

    @GetMapping("/movies")
    public Result<List<MovieCard>> movies() {
        return Result.ok(movieService.adminMovies());
    }

    @GetMapping("/movies/{id}")
    public Result<MovieCard> movie(@PathVariable Long id) {
        return Result.ok(movieService.movie(id));
    }

    @PostMapping("/movies")
    public Result<MovieCard> createMovie(@RequestBody Map<String, Object> payload) {
        return Result.ok(movieService.createMovie(payload));
    }

    @PutMapping("/movies/{id}")
    public Result<MovieCard> updateMovie(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return Result.ok(movieService.updateMovie(id, payload));
    }

    @DeleteMapping("/movies/{id}")
    public Result<Void> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return Result.ok();
    }

    @GetMapping("/homepage-recommendations")
    public Result<Map<String, Object>> homepageRecommendations() {
        return Result.ok(homepageRecommendationService.adminModel());
    }

    @PutMapping("/homepage-recommendations/{sectionCode}")
    public Result<Map<String, Object>> saveHomepageRecommendation(@PathVariable String sectionCode, @RequestBody Map<String, Object> payload) {
        return Result.ok(homepageRecommendationService.saveSection(sectionCode, payload));
    }

    @GetMapping("/cinemas")
    public Result<List<Map<String, Object>>> cinemas() {
        return Result.ok(movieService.adminCinemas());
    }

    @PostMapping("/cinemas")
    public Result<Map<String, Object>> createCinema(@RequestBody Map<String, Object> payload) {
        return Result.ok(movieService.createCinema(payload));
    }

    @PutMapping("/cinemas/{id}")
    public Result<Map<String, Object>> updateCinema(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return Result.ok(movieService.updateCinema(id, payload));
    }

    @DeleteMapping("/cinemas/{id}")
    public Result<Void> deleteCinema(@PathVariable Long id) {
        movieService.deleteCinema(id);
        return Result.ok();
    }

    @GetMapping("/performances/{id}/detail-blocks")
    public Result<List<Map<String, Object>>> detailBlocks(@PathVariable Long id) {
        return Result.ok(performanceService.detailBlocks(id));
    }

    @PostMapping("/performances/{id}/detail-blocks")
    public Result<Map<String, Object>> createDetailBlock(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return Result.ok(performanceService.createDetailBlock(id, payload));
    }

    @PutMapping("/performance-detail-blocks/{blockId}")
    public Result<Map<String, Object>> updateDetailBlock(@PathVariable Long blockId, @RequestBody Map<String, Object> payload) {
        return Result.ok(performanceService.updateDetailBlock(blockId, payload));
    }

    @DeleteMapping("/performance-detail-blocks/{blockId}")
    public Result<Void> deleteDetailBlock(@PathVariable Long blockId) {
        performanceService.deleteDetailBlock(blockId);
        return Result.ok();
    }

    @PutMapping("/performances/{id}/detail-blocks/reorder")
    public Result<List<Map<String, Object>>> reorderDetailBlocks(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return Result.ok(performanceService.reorderDetailBlocks(id, payload));
    }

    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择图片文件");
        }
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String suffix = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0) {
            suffix = originalName.substring(dot).toLowerCase(Locale.ROOT);
        }
        if (!List.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg").contains(suffix)) {
            suffix = ".jpg";
        }
        Path target = newUploadTarget(originalName);
        file.transferTo(target);
        return Result.ok(Map.of("path", "/uploads/admin/" + target.getFileName()));
    }

    @PostMapping("/upload/local-image")
    public Result<Map<String, Object>> uploadLocalImage(@RequestBody Map<String, Object> payload) throws IOException {
        String rawPath = String.valueOf(payload.getOrDefault("path", "")).trim();
        if ((rawPath.startsWith("\"") && rawPath.endsWith("\"")) || (rawPath.startsWith("'") && rawPath.endsWith("'"))) {
            rawPath = rawPath.substring(1, rawPath.length() - 1);
        }
        if (rawPath.isBlank()) {
            throw new IllegalArgumentException("请填写本机图片路径");
        }
        Path source = Paths.get(rawPath).normalize();
        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException("本机图片不存在或不可读取");
        }
        Path target = newUploadTarget(source.getFileName().toString());
        Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return Result.ok(Map.of("path", "/uploads/admin/" + target.getFileName()));
    }

    private Path newUploadTarget(String originalName) throws IOException {
        String suffix = "";
        int dot = originalName == null ? -1 : originalName.lastIndexOf('.');
        if (dot >= 0) {
            suffix = originalName.substring(dot).toLowerCase(Locale.ROOT);
        }
        if (!List.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg").contains(suffix)) {
            suffix = ".jpg";
        }
        Path dir = Paths.get("..", "uploads", "admin").normalize();
        Files.createDirectories(dir);
        return dir.resolve(UUID.randomUUID().toString().replace("-", "") + suffix);
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

    @PutMapping("/venues/{id}/disable")
    public Result<Void> disableVenue(@PathVariable Long id) {
        service.disableVenue(id);
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

    @DeleteMapping("/venues/{venueId}/seats")
    public Result<Map<String, Object>> clearVenueSeats(@PathVariable Long venueId) {
        int count = service.clearVenueSeats(venueId);
        return Result.ok(Map.of("deleted", count));
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
