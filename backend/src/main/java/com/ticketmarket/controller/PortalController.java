package com.ticketmarket.controller;

import com.ticketmarket.common.Result;
import com.ticketmarket.model.Category;
import com.ticketmarket.model.MovieCard;
import com.ticketmarket.model.PerformanceCard;
import com.ticketmarket.service.DemoDataService;
import com.ticketmarket.service.PersistentMovieService;
import com.ticketmarket.service.PersistentPerformanceService;
import com.ticketmarket.service.Phase3ResourceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/portal")
public class PortalController {
    private final DemoDataService dataService;
    private final PersistentPerformanceService performanceService;
    private final PersistentMovieService movieService;
    private final Phase3ResourceService resourceService;

    public PortalController(DemoDataService dataService, PersistentPerformanceService performanceService, PersistentMovieService movieService, Phase3ResourceService resourceService) {
        this.dataService = dataService;
        this.performanceService = performanceService;
        this.movieService = movieService;
        this.resourceService = resourceService;
    }

    @GetMapping("/home")
    public Result<Map<String, Object>> home() {
        List<PerformanceCard> all = performanceService.publicPerformances().stream().map(this::withRealtimeSaleStatus).toList();
        List<PerformanceCard> featured = all.stream()
                .filter(item -> Boolean.TRUE.equals(item.getHomeRecommended()))
                .sorted(Comparator.comparing(item -> item.getHomeSort() == null ? 0 : item.getHomeSort()))
                .limit(8)
                .toList();
        List<PerformanceCard> hot = featured.isEmpty()
                ? all.stream().filter(item -> "ON_SALE".equals(item.getSaleStatus())).limit(8).toList()
                : featured;
        List<PerformanceCard> coming = all.stream().filter(item -> "COMING_SOON".equals(item.getSaleStatus())).limit(6).toList();
        List<Map<String, Object>> categorySections = dataService.categories().stream()
                .filter(category -> !"movie".equals(category.code()))
                .map(category -> Map.<String, Object>of(
                        "code", category.code(),
                        "name", category.name(),
                        "items", all.stream().filter(item -> Objects.equals(item.getCategoryCode(), category.code())).limit(4).toList()
                ))
                .filter(section -> !((List<?>) section.get("items")).isEmpty())
                .limit(6)
                .toList();
        List<String> hotCities = allCities(all);
        List<String> hotVenues = distinctPerformanceValues(all, false);
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
                "categorySections", categorySections,
                "hotCities", hotCities.isEmpty() ? List.of("上海", "杭州", "南京", "深圳", "北京") : hotCities,
                "hotVenues", hotVenues.isEmpty() ? List.of("滨江音乐中心", "湖畔剧院", "紫金艺术厅", "云顶体育馆") : hotVenues,
                "movies", movieService.movies().stream().limit(6).toList()
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
        String normalizedKeyword = normalizeSearch(keyword);
        List<PerformanceCard> all = performanceService.publicPerformances().stream().map(this::withRealtimeSaleStatus).toList();
        List<PerformanceCard> performances = all.stream()
                .filter(item -> normalizedKeyword.isBlank()
                        || normalizeSearch(item.getTitle()).contains(normalizedKeyword)
                        || normalizeSearch(item.getSubtitle()).contains(normalizedKeyword)
                        || normalizeSearch(item.getSummary()).contains(normalizedKeyword)
                        || normalizeSearch(item.getVenue()).contains(normalizedKeyword)
                        || normalizeSearch(item.getAddress()).contains(normalizedKeyword)
                        || normalizeSearch(item.getCity()).contains(normalizedKeyword)
                        || normalizeSearch(item.getCategoryName()).contains(normalizedKeyword))
                .filter(item -> city == null || city.isBlank() || safe(item.getCity()).trim().equals(city.trim()))
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
                        "cities", allCities(all),
                        "statuses", List.of("ON_SALE", "COMING_SOON", "SOLD_OUT", "ENDED")
                )
        ));
    }

    @GetMapping("/performances/{id}")
    public Result<PerformanceCard> performanceDetail(@PathVariable Long id) {
        return Result.ok(withRealtimeSaleStatus(performanceService.publicPerformance(id)));
    }

    @GetMapping("/movies/{id}")
    public Result<MovieCard> movieDetail(@PathVariable Long id) {
        return Result.ok(movieService.movie(id));
    }

    private List<String> allCities(List<PerformanceCard> performances) {
        LinkedHashSet<String> values = new LinkedHashSet<>(distinctPerformanceValues(performances, true));
        resourceService.venues().stream()
                .map(item -> Objects.toString(item.get("cityName"), ""))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .forEach(values::add);
        return values.stream().limit(50).toList();
    }

    private List<String> distinctPerformanceValues(List<PerformanceCard> items, boolean city) {
        return items.stream()
                .map(item -> city ? item.getCity() : item.getVenue())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .limit(50)
                .toList();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalizeSearch(String value) {
        if (value == null) return "";
        return value.toLowerCase()
                .replaceAll("[\\p{P}\\p{S}\\s　]+", "")
                .trim();
    }

    private PerformanceCard withRealtimeSaleStatus(PerformanceCard card) {
        Map<String, Object> status = resourceService.frontPerformanceStatus(card.getId());
        card.setSaleStatus(Objects.toString(status.get("status"), card.getSaleStatus()));
        return card;
    }
}
