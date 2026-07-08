package com.ticketmarket.controller;

import com.ticketmarket.common.Result;
import com.ticketmarket.model.Category;
import com.ticketmarket.model.MovieCard;
import com.ticketmarket.model.PerformanceCard;
import com.ticketmarket.service.DemoDataService;
import com.ticketmarket.service.HomepageRecommendationService;
import com.ticketmarket.service.PersistentMovieService;
import com.ticketmarket.service.PersistentPerformanceService;
import com.ticketmarket.service.Phase3ResourceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
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
    private final HomepageRecommendationService homepageRecommendationService;

    public PortalController(DemoDataService dataService,
                            PersistentPerformanceService performanceService,
                            PersistentMovieService movieService,
                            Phase3ResourceService resourceService,
                            HomepageRecommendationService homepageRecommendationService) {
        this.dataService = dataService;
        this.performanceService = performanceService;
        this.movieService = movieService;
        this.resourceService = resourceService;
        this.homepageRecommendationService = homepageRecommendationService;
    }

    @GetMapping("/home")
    public Result<Map<String, Object>> home() {
        List<PerformanceCard> all = performanceService.publicPerformances().stream()
                .map(this::withRealtimeSaleStatus)
                .toList();
        List<MovieCard> movies = movieService.movies();
        Map<String, Object> recommendations = homepageRecommendationService.portalRecommendations(all, movies);
        List<PerformanceCard> coming = all.stream()
                .filter(item -> "COMING_SOON".equals(item.getSaleStatus()))
                .limit(4)
                .toList();
        List<Map<String, Object>> hotItems = recommendationItems(recommendations.get("hot"));
        return Result.ok(Map.of(
                "banners", recommendations.get("banners"),
                "categories", dataService.categories(),
                "hot", recommendations.get("hot"),
                "comingSoon", coming,
                "onSale", all.stream().filter(item -> "ON_SALE".equals(item.getSaleStatus())).limit(4).toList(),
                "categorySections", recommendations.get("categorySections"),
                "hotCities", hotValues(hotItems, all, true),
                "hotVenues", hotValues(hotItems, all, false),
                "movies", recommendations.get("movies")
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

    @GetMapping("/movies")
    public Result<List<MovieCard>> movies() {
        return Result.ok(movieService.movies());
    }

    @GetMapping("/movies/{id}/schedule")
    public Result<Map<String, Object>> movieSchedule(@PathVariable Long id, @RequestParam(required = false) String city) {
        return Result.ok(movieService.movieSchedule(id, city));
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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> recommendationItems(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .toList();
    }

    private List<String> hotValues(List<Map<String, Object>> hotItems, List<PerformanceCard> performances, boolean city) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        String key = city ? "city" : "venue";
        for (Map<String, Object> item : hotItems) {
            addHotValue(values, Objects.toString(item.get(key), ""));
            if (values.size() >= 4) return values.stream().toList();
        }
        for (String value : rankedUnendedValues(performances, city)) {
            addHotValue(values, value);
            if (values.size() >= 4) break;
        }
        return values.stream().limit(4).toList();
    }

    private void addHotValue(LinkedHashSet<String> values, String value) {
        String normalized = value == null ? "" : value.trim();
        if (!normalized.isBlank()) values.add(normalized);
    }

    private List<String> rankedUnendedValues(List<PerformanceCard> performances, boolean city) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (PerformanceCard item : performances) {
            if ("ENDED".equals(item.getSaleStatus())) continue;
            String value = city ? item.getCity() : item.getVenue();
            if (value == null || value.trim().isBlank()) continue;
            counts.merge(value.trim(), 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
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
