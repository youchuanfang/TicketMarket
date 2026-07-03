package com.ticketmarket.controller;

import com.ticketmarket.common.Result;
import com.ticketmarket.model.Category;
import com.ticketmarket.model.MovieCard;
import com.ticketmarket.model.PerformanceCard;
import com.ticketmarket.service.DemoDataService;
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

    public PortalController(DemoDataService dataService) {
        this.dataService = dataService;
    }

    @GetMapping("/home")
    public Result<Map<String, Object>> home() {
        List<PerformanceCard> all = dataService.performances();
        List<PerformanceCard> hot = all.stream().filter(item -> "ON_SALE".equals(item.getSaleStatus())).limit(5).toList();
        List<PerformanceCard> coming = all.stream().filter(item -> "COMING_SOON".equals(item.getSaleStatus())).limit(4).toList();
        List<PerformanceCard> inventoryUpdates = all.stream().filter(item -> "RETURNED".equals(item.getSaleStatus()) || "LOCKED".equals(item.getSaleStatus())).limit(4).toList();
        return Result.ok(Map.of(
                "banners", List.of(
                        Map.of("title", "夏日票务季", "subtitle", "演唱会、话剧、电影和展览一站式购票", "image", "/posters/performance/poster-101.svg", "targetId", 101),
                        Map.of("title", "剧院好座开放", "subtitle", "热门话剧支持按场次和票档浏览", "image", "/posters/performance/poster-102.svg", "targetId", 102),
                        Map.of("title", "票量动态提醒", "subtitle", "关注热门项目状态，及时查看可购票档", "image", "/posters/performance/poster-105.svg", "targetId", 105)
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
        List<PerformanceCard> performances = dataService.search(keyword, city, category, status)
                .stream()
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
        return Result.ok(dataService.performance(id));
    }

    @GetMapping("/movies/{id}")
    public Result<MovieCard> movieDetail(@PathVariable Long id) {
        return Result.ok(dataService.movie(id));
    }
}
