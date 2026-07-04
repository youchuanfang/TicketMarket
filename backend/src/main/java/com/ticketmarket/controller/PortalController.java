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
        List<PerformanceCard> inventoryUpdates = all.stream().filter(item -> "RETURNED".equals(item.getSaleStatus()) || "LOCKED".equals(item.getSaleStatus())).limit(4).toList();
        return Result.ok(Map.of(
                "banners", List.of(
                        Map.of("title", "夏日城市舞台", "subtitle", "演出、电影、展览一站式发现", "image", "/uploads/banners/banner-01.svg", "targetId", 101),
                        Map.of("title", "周末剧场计划", "subtitle", "精选场次与舒适票档", "image", "/uploads/banners/banner-02.svg", "targetId", 102),
                        Map.of("title", "星河音乐现场", "subtitle", "灯光与旋律同步开场", "image", "/uploads/banners/banner-03.svg", "targetId", 103),
                        Map.of("title", "亲子幻想日", "subtitle", "全家共享的轻松观演", "image", "/uploads/banners/banner-04.svg", "targetId", 107),
                        Map.of("title", "热血运动夜", "subtitle", "看台视角与现场欢呼", "image", "/uploads/banners/banner-05.svg", "targetId", 108),
                        Map.of("title", "光影艺术季", "subtitle", "沉浸展览与城市漫游", "image", "/uploads/banners/banner-06.svg", "targetId", 109)
                ),
                "categories", dataService.categories(),
                "hot", hot,
                "comingSoon", coming,
                "onSale", hot,
                "returned", inventoryUpdates,
                "hotCities", List.of("上海", "杭州", "南京", "深圳"),
                "hotVenues", List.of("滨江音乐中心", "湖畔剧院", "紫金艺术厅", "云顶体育馆"),
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
        List<PerformanceCard> performances = performanceService.publicPerformances()
                .stream()
                .filter(item -> keyword == null || keyword.isBlank()
                        || item.getTitle().toLowerCase().contains(keyword.trim().toLowerCase())
                        || item.getVenue().toLowerCase().contains(keyword.trim().toLowerCase())
                        || item.getCategoryName().toLowerCase().contains(keyword.trim().toLowerCase()))
                .filter(item -> city == null || city.isBlank() || item.getCity().equals(city))
                .filter(item -> category == null || category.isBlank()
                        || item.getCategoryCode().equals(category)
                        || item.getCategoryName().equals(category))
                .filter(item -> status == null || status.isBlank() || item.getSaleStatus().equals(status))
                .sorted(Comparator.comparing(PerformanceCard::getStartTime))
                .toList();
        return Result.ok(Map.of(
                "total", performances.size(),
                "items", performances,
                "filters", Map.of(
                        "cities", List.of("上海", "杭州", "南京", "深圳"),
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
}
