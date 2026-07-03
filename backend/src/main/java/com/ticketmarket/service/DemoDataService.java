package com.ticketmarket.service;

import com.ticketmarket.common.ApiException;
import com.ticketmarket.model.Category;
import com.ticketmarket.model.MovieCard;
import com.ticketmarket.model.PerformanceCard;
import com.ticketmarket.model.SessionOption;
import com.ticketmarket.model.TicketLevel;
import com.ticketmarket.model.UserAccount;
import com.ticketmarket.model.Viewer;
import com.ticketmarket.util.PasswordUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class DemoDataService {
    private final AtomicLong userId = new AtomicLong(20);
    private final AtomicLong viewerId = new AtomicLong(100);
    private final Map<Long, UserAccount> usersById = new ConcurrentHashMap<>();
    private final Map<String, UserAccount> usersByName = new ConcurrentHashMap<>();
    private final Map<Long, List<Viewer>> viewersByUser = new ConcurrentHashMap<>();
    private final List<Category> categories = new ArrayList<>();
    private final List<PerformanceCard> performances = new ArrayList<>();
    private final List<MovieCard> movies = new ArrayList<>();

    @PostConstruct
    public void init() {
        seedUsers();
        seedCategories();
        seedPerformances();
        seedMovies();
    }

    public UserAccount createUser(String username, String password, String nickname) {
        String normalized = normalize(username);
        if (usersByName.containsKey(normalized)) {
            throw new ApiException(409, "用户名已存在");
        }
        UserAccount account = new UserAccount(userId.incrementAndGet(), normalized, PasswordUtil.hash(password), nickname, "USER", false);
        usersById.put(account.getId(), account);
        usersByName.put(account.getUsername(), account);
        viewersByUser.put(account.getId(), new ArrayList<>());
        return account;
    }

    public Optional<UserAccount> findUserByName(String username) {
        return Optional.ofNullable(usersByName.get(normalize(username)));
    }

    public Optional<UserAccount> findUserById(Long id) {
        return Optional.ofNullable(usersById.get(id));
    }

    public List<Viewer> listViewers(Long userId) {
        return List.copyOf(viewersByUser.getOrDefault(userId, List.of()));
    }

    public Viewer addViewer(Long userId, String name, String idCard, String phone) {
        Viewer viewer = new Viewer(viewerId.incrementAndGet(), userId, name, maskIdCard(idCard), maskPhone(phone));
        List<Viewer> viewers = viewersByUser.computeIfAbsent(userId, key -> new ArrayList<>());
        if (viewers.isEmpty()) {
            viewer.setDefaultViewer(true);
        }
        viewers.add(viewer);
        return viewer;
    }

    public Viewer updateViewer(Long userId, Long targetViewerId, String name, String idCard, String phone) {
        Viewer viewer = findViewer(userId, targetViewerId);
        viewer.setName(name);
        viewer.setIdCardMasked(maskIdCard(idCard));
        viewer.setPhoneMasked(maskPhone(phone));
        return viewer;
    }

    public void deleteViewer(Long userId, Long targetViewerId) {
        List<Viewer> viewers = viewersByUser.getOrDefault(userId, new ArrayList<>());
        Viewer viewer = findViewer(userId, targetViewerId);
        boolean wasDefault = viewer.isDefaultViewer();
        viewers.removeIf(item -> Objects.equals(item.getId(), targetViewerId));
        if (wasDefault && !viewers.isEmpty()) {
            viewers.get(0).setDefaultViewer(true);
        }
    }

    public Viewer setDefaultViewer(Long userId, Long targetViewerId) {
        List<Viewer> viewers = viewersByUser.getOrDefault(userId, new ArrayList<>());
        Viewer target = findViewer(userId, targetViewerId);
        viewers.forEach(item -> item.setDefaultViewer(false));
        target.setDefaultViewer(true);
        return target;
    }

    public List<Category> categories() {
        return categories.stream().sorted(Comparator.comparingInt(Category::sortOrder)).toList();
    }

    public List<PerformanceCard> performances() {
        return List.copyOf(performances);
    }

    public PerformanceCard performance(Long id) {
        return performances.stream()
                .filter(item -> Objects.equals(item.getId(), id))
                .findFirst()
                .orElseThrow(() -> new ApiException(404, "演出不存在"));
    }

    public MovieCard movie(Long id) {
        return movies.stream()
                .filter(item -> Objects.equals(item.getId(), id))
                .findFirst()
                .orElseThrow(() -> new ApiException(404, "电影不存在"));
    }

    public List<MovieCard> movies() {
        return List.copyOf(movies);
    }

    public List<PerformanceCard> search(String keyword, String city, String category, String status) {
        String kw = normalizeNullable(keyword);
        String cityValue = normalizeNullable(city);
        String categoryValue = normalizeNullable(category);
        String statusValue = normalizeNullable(status);
        return performances.stream()
                .filter(item -> kw.isBlank()
                        || normalize(item.getTitle()).contains(kw)
                        || normalize(item.getVenue()).contains(kw)
                        || normalize(item.getCategoryName()).contains(kw))
                .filter(item -> cityValue.isBlank() || normalize(item.getCity()).equals(cityValue))
                .filter(item -> categoryValue.isBlank() || normalize(item.getCategoryCode()).equals(categoryValue))
                .filter(item -> statusValue.isBlank() || normalize(item.getSaleStatus()).equals(statusValue))
                .collect(Collectors.toList());
    }

    private Viewer findViewer(Long userId, Long targetViewerId) {
        return viewersByUser.getOrDefault(userId, List.of()).stream()
                .filter(item -> Objects.equals(item.getId(), targetViewerId))
                .findFirst()
                .orElseThrow(() -> new ApiException(404, "观演人不存在"));
    }

    private void seedUsers() {
        addDefaultUser(1L, "admin", "admin123", "系统管理员", "ADMIN", true);
        addDefaultUser(2L, "manager", "manager123", "票务管理员", "MANAGER", true);
        addDefaultUser(3L, "checker", "checker123", "检票员", "CHECKER", true);
        addDefaultUser(4L, "user01", "user123", "普通用户一", "USER", true);
        addDefaultUser(5L, "user02", "user123", "普通用户二", "USER", true);
        addDefaultUser(6L, "user03", "user123", "普通用户三", "USER", true);
        addDefaultUser(7L, "user04", "user123", "普通用户四", "USER", true);
        addDefaultUser(8L, "user05", "user123", "普通用户五", "USER", true);
        addDefaultUser(9L, "user06", "user123", "普通用户六", "USER", true);
        addDefaultUser(10L, "user07", "user123", "普通用户七", "USER", true);
        Viewer viewer = new Viewer(1L, 4L, "林晓", "3301**********2245", "138****1024");
        viewer.setDefaultViewer(true);
        viewersByUser.put(4L, new ArrayList<>(List.of(viewer)));
        Viewer demoViewer = new Viewer(2L, 6L, "周晴", "3301**********2246", "139****1234");
        demoViewer.setDefaultViewer(true);
        viewersByUser.put(6L, new ArrayList<>(List.of(demoViewer)));
    }

    private void addDefaultUser(Long id, String username, String password, String nickname, String roleCode, boolean verified) {
        UserAccount account = new UserAccount(id, username, PasswordUtil.hash(password), nickname, roleCode, verified);
        usersById.put(id, account);
        usersByName.put(username, account);
        viewersByUser.putIfAbsent(id, new ArrayList<>());
    }

    private void seedCategories() {
        categories.add(new Category("movie", "电影", "VideoCamera", 1));
        categories.add(new Category("concert", "演唱会", "Microphone", 2));
        categories.add(new Category("drama", "话剧歌剧", "Tickets", 3));
        categories.add(new Category("music", "音乐会", "Headphones", 4));
        categories.add(new Category("sports", "体育赛事", "Trophy", 5));
        categories.add(new Category("family", "儿童亲子", "Star", 6));
        categories.add(new Category("exhibition", "展览休闲", "Collection", 7));
        categories.add(new Category("quyi", "曲苑杂坛", "Reading", 8));
        categories.add(new Category("dance", "舞蹈芭蕾", "MagicStick", 9));
        categories.add(new Category("anime", "二次元", "Sugar", 10));
        categories.add(new Category("travel", "旅游展览", "Location", 11));
        categories.add(new Category("festival", "音乐节", "Sunny", 12));
    }

    private void seedPerformances() {
        performances.add(performance(101L, "星河回声巡回演唱会", "concert", "演唱会", "上海", "滨江音乐中心",
                "浦东新区临江大道 88 号", "2026-08-18 19:30", 380, 1280, "AUTO_ALLOCATE", "ON_SALE",
                List.of("实名制", "电子票", "限购2张", "不支持选座"),
                "沉浸式声光舞台与热门曲目串联，适合热门演唱会购票。"));
        performances.add(performance(102L, "城市剧场·夜航西窗", "drama", "话剧歌剧", "杭州", "湖畔剧院",
                "西湖区文艺路 16 号", "2026-08-21 19:00", 180, 680, "SELECTABLE", "ON_SALE",
                List.of("实名制", "支持选座", "条件退", "电子票"),
                "小剧场悬疑话剧，支持按场次和票档浏览。"));
        performances.add(performance(103L, "仲夏室内乐精选", "music", "音乐会", "南京", "紫金艺术厅",
                "玄武区中央路 201 号", "2026-08-09 20:00", 120, 520, "AREA_ONLY", "COMING_SOON",
                List.of("实名制", "区域购票", "电子票"),
                "古典室内乐精选曲目，适合预约开售提醒。"));
        performances.add(performance(104L, "城市冠军杯篮球邀请赛", "sports", "体育赛事", "上海", "云顶体育馆",
                "闵行区体育公园 6 号", "2026-09-05 19:35", 99, 599, "SELECTABLE", "ON_SALE",
                List.of("支持选座", "电子票", "限购4张"),
                "主客队对抗与分区票档，适合体育赛事购票展示。"));
        performances.add(performance(105L, "未来城市互动展", "exhibition", "展览休闲", "深圳", "湾区会展中心",
                "南山区科技南路 99 号", "2026-07-25 10:00", 68, 168, "STANDING", "RETURNED",
                List.of("电子票", "票量紧张", "无需选座"),
                "科技、建筑与数字艺术结合的展览项目。"));
        performances.add(performance(106L, "童梦森林亲子剧", "family", "儿童亲子", "杭州", "湖畔剧院",
                "西湖区文艺路 16 号", "2026-08-02 15:00", 80, 320, "SELECTABLE", "ON_SALE",
                List.of("支持选座", "亲子套票", "条件退"),
                "适合家庭观演的轻量票务场景。"));
        performances.add(performance(107L, "次元夏日嘉年华", "anime", "二次元", "南京", "紫金展览馆",
                "建邺区江东中路 300 号", "2026-08-30 09:30", 99, 299, "STANDING", "COMING_SOON",
                List.of("站席", "电子票", "开售提醒"),
                "二次元主题活动，支持站席票档展示。"));
        performances.add(performance(108L, "海风音乐节双日通票", "festival", "音乐节", "深圳", "湾区草坪剧场",
                "南山区滨海大道 188 号", "2026-09-12 14:00", 399, 899, "AUTO_ALLOCATE", "LOCKED",
                List.of("暂不可售", "不支持选座", "电子票"),
                "双日音乐节通票项目，后续票档开放安排请关注项目页。"));
    }

    private PerformanceCard performance(Long id, String title, String categoryCode, String categoryName, String city,
                                        String venue, String address, String startTime, int priceMin, int priceMax,
                                        String saleMode, String status, List<String> tags, String summary) {
        PerformanceCard card = new PerformanceCard();
        card.setId(id);
        card.setTitle(title);
        card.setCategoryCode(categoryCode);
        card.setCategoryName(categoryName);
        card.setCity(city);
        card.setVenue(venue);
        card.setAddress(address);
        card.setStartTime(startTime);
        card.setPriceMin(priceMin);
        card.setPriceMax(priceMax);
        card.setPoster("/posters/performance/poster-" + id + ".svg");
        card.setSaleStatus(status);
        card.setSaleMode(saleMode);
        card.setTags(tags);
        card.setSummary(summary);
        card.setIntro(summary + " 平台提供场次、票档、电子票和入场规则等完整票务信息。");
        card.setArtistInfo("演职人员信息以主办方发布内容为准，具体阵容请关注平台公告。");
        card.setVenueIntro(venue + " 交通便利，场馆内设置清晰的分区指引和入场通道。");
        card.setPurchaseNotice("请先登录并完成实名信息；每个账号和身份证按售票批次限购。");
        card.setRefundRule("演出开始 72 小时前可退，24-72 小时收取 20% 手续费，24 小时内不可退。");
        card.setEntryRule("电子票核验通过后入场，已退票、已核验或非当前场次票不可入场。");
        card.setSessions(List.of(
                new SessionOption(id * 10 + 1, startTime, "2026-07-20 10:00", "2026-08-01 18:00", "主厅", saleMode),
                new SessionOption(id * 10 + 2, startTime.replace("19:30", "14:30").replace("19:00", "14:30"), "2026-07-27 10:00", "2026-08-02 18:00", "加场厅", saleMode)
        ));
        card.setTicketLevels(List.of(
                new TicketLevel(id * 100 + 1, "看台票", "A区", priceMin, 120),
                new TicketLevel(id * 100 + 2, "优选票", "B区", Math.min(priceMax, priceMin + 200), 80),
                new TicketLevel(id * 100 + 3, "臻享票", "C区", priceMax, 30)
        ));
        return card;
    }

    private void seedMovies() {
        movies.add(movie(201L, "深空旅人", "科幻/冒险", "2026-07-18", 128, "陈一川", "周宁、许知夏"));
        movies.add(movie(202L, "夏日邮局", "剧情/爱情", "2026-07-26", 112, "林青禾", "赵予安、梁辰"));
        movies.add(movie(203L, "山海小队", "动画/家庭", "2026-08-01", 96, "韩砚", "配音：白露、沈朗"));
        movies.add(movie(204L, "逆风终点线", "运动/励志", "2026-08-09", 118, "宋桥", "顾晨、邵雨"));
        movies.add(movie(205L, "夜色档案", "悬疑/犯罪", "2026-08-16", 124, "叶舟", "秦澈、沈禾"));
    }

    private MovieCard movie(Long id, String title, String genre, String releaseDate, int duration, String director, String actors) {
        MovieCard movie = new MovieCard();
        movie.setId(id);
        movie.setTitle(title);
        movie.setGenre(genre);
        movie.setReleaseDate(releaseDate);
        movie.setDurationMinutes(duration);
        movie.setDirector(director);
        movie.setActors(actors);
        movie.setPoster("/posters/movie/movie-" + id + ".svg");
        movie.setSummary("影片正在热映，支持按影院和场次选择座位购票。");
        movie.setSessions(List.of(
                new SessionOption(id * 10 + 1, releaseDate + " 10:30", "2026-07-10 10:00", "2026-12-31 23:00", "1号厅", "SELECTABLE"),
                new SessionOption(id * 10 + 2, releaseDate + " 19:40", "2026-07-10 10:00", "2026-12-31 23:00", "2号厅", "SELECTABLE")
        ));
        return movie;
    }

    private String normalizeNullable(String value) {
        return value == null ? "" : normalize(value);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String maskIdCard(String idCard) {
        String value = idCard == null ? "" : idCard.trim();
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "**********" + value.substring(value.length() - 4);
    }

    private String maskPhone(String phone) {
        String value = phone == null ? "" : phone.trim();
        if (value.length() < 7) {
            return "****";
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }
}
