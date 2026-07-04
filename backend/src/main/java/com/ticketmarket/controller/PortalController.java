package com.ticketmarket.controller;

import com.ticketmarket.common.Result;
import com.ticketmarket.model.Category;
import com.ticketmarket.model.MovieCard;
import com.ticketmarket.model.PerformanceCard;
import com.ticketmarket.service.DemoDataService;
import com.ticketmarket.service.PersistentPerformanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/portal")
public class PortalController {
    private final DemoDataService dataService;
    private final PersistentPerformanceService performanceService;

    public PortalController(DemoDataService dataService, PersistentPerformanceService performanceService) {
        this.dataService = dataService;
        this.performanceService = performanceService;
    }

    @GetMapping("/home")
    public Result<Map<String, Object>> home() {
        List<PerformanceCard> all = performanceService.publicPerformances();
        List<PerformanceCard> hot = all.stream().filter(item -> "ON_SALE".equals(item.getSaleStatus())).limit(5).toList();
        List<PerformanceCard> coming = all.stream().filter(item -> "COMING_SOON".equals(item.getSaleStatus())).limit(4).toList();
        List<PerformanceCard> inventoryUpdates = all.stream()
                .filter(item -> "RETURNED".equals(item.getSaleStatus()) || "LOCKED".equals(item.getSaleStatus()))
                .limit(4)
                .toList();
        List<String> hotCities = distinctValues(all, true);
        List<String> hotVenues = distinctValues(all, false);
        return Result.ok(Map.of(
                "banners", List.of(
                        Map.of("title", "夏日城市舞台", "subtitle", "演出、电影、展览一站式发现", "image", "/uploads/banners/banner-01.svg", "targetId", 101),
                        Map.of("title", "周末剧场计划", "subtitle", "精选场次与舒适票档", "image", "/uploads/banners/banner-02.svg", "targetId", 102),
                        Map.of("title", "星河音乐现场", "subtitle", "灯光与旋律同步开场", "image", "/uploads/banners/banner-03.svg", "targetId", 103)
                ),
                "categories", dataService.categories(),
                "hot", hot,
                "comingSoon", coming,
                "onSale", hot,
                "returned", inventoryUpdates,
                "hotCities", hotCities.isEmpty() ? List.of("上海", "杭州", "南京", "深圳") : hotCities,
                "hotVenues", hotVenues.isEmpty() ? List.of("滨江音乐中心", "湖畔剧院", "紫金艺术厅", "云顶体育馆") : hotVenues,
                "movies", dataService.movies().stream().limit(5).toList()
        ));
    }

    @GetMapping("/categories")
    public Result<List<Category>> categories() {
        return Result.ok(dataService.categories());
    }

    @GetMapping("/search")
    public Result<Map<String, Object>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status
    ) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        List<PerformanceCard> all = performanceService.publicPerformances();
        List<PerformanceCard> performances = all.stream()
                .filter(item -> normalizedKeyword.isBlank()
                        || safe(item.getTitle()).toLowerCase().contains(normalizedKeyword)
                        || safe(item.getVenue()).toLowerCase().contains(normalizedKeyword)
                        || safe(item.getCategoryName()).toLowerCase().contains(normalizedKeyword))
                .filter(item -> city == null || city.isBlank() || safe(item.getCity()).equals(city))
                .filter(item -> category == null || category.isBlank()
                        || safe(item.getCategoryCode()).equals(category)
                        || safe(item.getCategoryName()).equals(category))
                .filter(item -> status == null || status.isBlank() || safe(item.getSaleStatus()).equals(status))
                .sorted(Comparator.comparing(PerformanceCard::getStartTime))
                .toList();
        return Result.ok(Map.of(
                "total", performances.size(),
                "items", performances,
                "filters", Map.of(
                        "cities", distinctValues(all, true),
                        "statuses", List.of("ON_SALE", "COMING_SOON", "RETURNED", "LOCKED")
                )
        ));
    }

    @GetMapping("/performances/{id}")
    public Result<PerformanceCard> performanceDetail(@PathVariable Long id) {
        return Result.ok(performanceService.publicPerformance(id));
    }

    @GetMapping("/movies/{id}")
    public Result<MovieCard> movieDetail(@PathVariable Long id) {
        return Result.ok(dataService.movie(id));
    }

    private List<String> distinctValues(List<PerformanceCard> items, boolean city) {
        return items.stream()
                .map(item -> city ? item.getCity() : item.getVenue())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .limit(12)
                .toList();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
