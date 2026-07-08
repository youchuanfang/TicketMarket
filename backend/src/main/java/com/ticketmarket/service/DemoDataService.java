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
    private final AtomicLong performanceId = new AtomicLong(112);
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
        return createAccount(username, password, nickname, "USER", false);
    }

    public UserAccount createStaffUser(String username, String password, String nickname, String roleCode) {
        String role = normalizeRole(roleCode);
        if (!List.of("ADMIN", "MANAGER", "CHECKER").contains(role)) {
            throw new ApiException(400, "只能创建管理员或检票员账号");
        }
        return createAccount(username, password, nickname, role, true);
    }

    private UserAccount createAccount(String username, String password, String nickname, String roleCode, boolean verified) {
        String normalized = normalize(username);
        if (usersByName.containsKey(normalized)) {
            throw new ApiException(409, "用户名已存在");
        }
        UserAccount account = new UserAccount(userId.incrementAndGet(), normalized, PasswordUtil.hash(password), nickname, roleCode, verified);
        usersById.put(account.getId(), account);
        usersByName.put(account.getUsername(), account);
        viewersByUser.put(account.getId(), new ArrayList<>());
        return account;
    }

    private String normalizeRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) return "CHECKER";
        return roleCode.trim().toUpperCase(Locale.ROOT);
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
        return performances.stream()
                .filter(item -> !"DRAFT".equals(item.getPublishStatus()))
                .toList();
    }

    public List<PerformanceCard> adminPerformances() {
        return List.copyOf(performances);
    }

    public PerformanceCard performance(Long id) {
        return performances.stream()
                .filter(item -> Objects.equals(item.getId(), id))
                .findFirst()
                .orElseThrow(() -> new ApiException(404, "演出不存在"));
    }

    public synchronized PerformanceCard createPerformance(Map<String, Object> payload) {
        PerformanceCard card = new PerformanceCard();
        card.setId(performanceId.incrementAndGet());
        applyPerformancePayload(card, payload);
        performances.add(0, card);
        return card;
    }

    public synchronized PerformanceCard updatePerformance(Long id, Map<String, Object> payload) {
        PerformanceCard card = performance(id);
        applyPerformancePayload(card, payload);
        return card;
    }

    public synchronized void deletePerformance(Long id) {
        performance(id).setPublishStatus("DRAFT");
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
        return performances().stream()
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
        performances.add(performance(101L, "星河回声巡回演唱会", "光束、合唱与城市夜景交织的声浪现场", "concert", "演唱会", "上海", "滨江音乐中心",
                "浦东新区临江大道 88 号", "2026-08-18 19:30", 380, 1280, "AUTO_ALLOCATE", "ON_SALE",
                List.of("实名制", "电子票", "限购2张", "自动配座")));
        performances.add(performance(102L, "城市剧场·夜航西窗", "一封夜班列车上的来信，引出城市边缘的温柔谜题", "drama", "话剧歌剧", "杭州", "湖畔剧院",
                "西湖区文艺路 16 号", "2026-08-21 19:00", 180, 680, "SELECTABLE", "ON_SALE",
                List.of("实名制", "支持选座", "条件退", "电子票")));
        performances.add(performance(103L, "仲夏室内乐精选", "弦乐、木管与钢琴在小厅中展开清澈对话", "music", "音乐会", "南京", "紫金艺术厅",
                "玄武区中央路 201 号", "2026-08-09 20:00", 120, 520, "AREA_ONLY", "COMING_SOON",
                List.of("实名制", "区域购票", "电子票", "开售提醒")));
        performances.add(performance(104L, "次元夏日嘉年华", "原创角色巡游、舞台互动与主题市集集合", "anime", "二次元", "南京", "紫金展览馆",
                "建邺区江东中路 300 号", "2026-08-30 09:30", 99, 299, "STANDING", "COMING_SOON",
                List.of("站席", "电子票", "开售提醒", "限购4张")));
        performances.add(performance(105L, "海风音乐节双日通票", "两日草坪舞台、落日合唱与海岸灯带", "festival", "音乐节", "深圳", "湾区草坪剧场",
                "南山区滨海大道 188 号", "2026-09-12 14:00", 399, 899, "AUTO_ALLOCATE", "LOCKED",
                List.of("暂不可售", "不支持选座", "电子票", "双日通票")));
        performances.add(performance(106L, "未来城市互动展", "用影像、装置和触控墙拼接下一站城市想象", "exhibition", "展览休闲", "深圳", "湾区会展中心",
                "南山区科技南路 99 号", "2026-07-25 10:00", 68, 168, "STANDING", "RETURNED",
                List.of("电子票", "票量紧张", "无需选座", "分时入场")));
        performances.add(performance(107L, "亲子幻想剧场", "纸船、星灯与森林伙伴组成的轻喜剧", "family", "儿童亲子", "杭州", "湖畔剧院",
                "西湖区文艺路 16 号", "2026-08-02 15:00", 80, 320, "SELECTABLE", "ON_SALE",
                List.of("支持选座", "亲子套票", "条件退", "电子票")));
        performances.add(performance(108L, "热血篮球挑战赛", "城市队伍邀请赛，近距离感受攻防节奏", "sports", "体育赛事", "上海", "云顶体育馆",
                "闵行区体育公园 6 号", "2026-09-05 19:35", 99, 599, "SELECTABLE", "ON_SALE",
                List.of("支持选座", "电子票", "限购4张", "分区入场")));
        performances.add(performance(109L, "光影沉浸艺术展", "流动影像、镜面空间与城市声景共同构成展线", "exhibition", "展览休闲", "深圳", "南山艺文仓",
                "南山区艺文路 28 号", "2026-09-18 10:00", 88, 228, "STANDING", "ON_SALE",
                List.of("电子票", "分时入场", "无需选座", "条件退")));
        performances.add(performance(110L, "周末脱口秀专场", "三组虚构喜剧演员带来轻松犀利的城市观察", "quyi", "曲苑杂坛", "上海", "梧桐小剧场",
                "徐汇区梧桐路 36 号", "2026-08-16 20:00", 120, 360, "AREA_ONLY", "ON_SALE",
                List.of("实名制", "区域购票", "电子票", "限购2张")));
        performances.add(performance(111L, "古典芭蕾精选夜", "以原创舞段串联湖畔、月光与晨雾三幕", "dance", "舞蹈芭蕾", "杭州", "镜湖大剧院",
                "拱墅区镜湖路 66 号", "2026-09-02 19:30", 180, 880, "SELECTABLE", "COMING_SOON",
                List.of("支持选座", "实名制", "电子票", "开售提醒")));
        performances.add(performance(112L, "国风民乐新声音乐会", "笛、阮、笙与电子氛围织成新民乐夜场", "music", "音乐会", "南京", "青砖音乐厅",
                "秦淮区青砖巷 12 号", "2026-09-09 19:30", 160, 680, "AUTO_ALLOCATE", "COMING_SOON",
                List.of("实名制", "电子票", "自动配座", "开售提醒")));
    }

    private PerformanceCard performance(Long id, String title, String categoryCode, String categoryName, String city,
                                        String venue, String address, String startTime, int priceMin, int priceMax,
                                        String saleMode, String status, List<String> tags, String summary) {
        return performance(id, title, summary, categoryCode, categoryName, city, venue, address, startTime, priceMin, priceMax, saleMode, status, tags);
    }

    private PerformanceCard performance(Long id, String title, String subtitle, String categoryCode, String categoryName, String city,
                                        String venue, String address, String startTime, int priceMin, int priceMax,
                                        String saleMode, String status, List<String> tags) {
        PerformanceCard card = new PerformanceCard();
        card.setId(id);
        card.setTitle(title);
        card.setSubtitle(subtitle);
        card.setCategoryCode(categoryCode);
        card.setCategoryName(categoryName);
        card.setVenueId(null);
        card.setCity(city);
        card.setVenue(venue);
        card.setAddress(address);
        card.setStartTime(startTime);
        card.setPriceMin(priceMin);
        card.setPriceMax(priceMax);
        card.setPoster("/uploads/posters/performance/poster-" + id + ".svg");
        card.setBanner("/uploads/banners/banner-" + String.format("%02d", ((id.intValue() - 101) % 6) + 1) + ".svg");
        card.setDetailImage("/uploads/detail/detail-" + String.format("%02d", ((id.intValue() - 101) % 8) + 1) + ".svg");
        card.setSaleStatus(status);
        card.setSaleMode(saleMode);
        card.setPublishStatus("PUBLISHED");
        card.setTags(tags);
        card.setSummary(subtitle);
        card.setIntro(title + "围绕“" + subtitle + "”展开，节目以清晰的段落、稳定的现场调度和富有层次的灯光声场组织观演体验。项目内容、人员与场馆均为 TicketMarket 本地虚构数据，用于展示正式票务平台中项目介绍、场次选择、票档展示和入场须知的完整链路。");
        card.setArtistInfo("本项目由创作团队“城市回声工作室”联合多位青年演职人员完成，阵容包含舞台统筹、声音设计、视觉导演与现场执行团队。演职人员姓名、团队履历和节目介绍均为本地原创虚构内容。");
        card.setVenueIntro(venue + "位于" + address + "，设有清晰的入口、休息区和分区导视。观众可根据票面信息提前规划交通，建议预留安检、取票或电子票核验时间，现场以项目页和站内通知公布的指引为准。");
        card.setPurchaseNotice("购票前请确认场次、票档、数量和观演人信息。电子票将在订单支付完成后生成，同一账号同一场次按项目规则限购；儿童、陪同人员及特殊入场要求请以本页说明和现场公告为准。");
        card.setRefundRule("本项目支持条件退票：距开场 72 小时以上可申请退票，距开场 24 至 72 小时内按订单金额收取服务费，距开场不足 24 小时或票券已核验后不支持退票。");
        card.setEntryRule("请携带购票账号对应的有效身份证件或电子票二维码入场。入场时需通过票券核验，已退票、已核验、非本场次或截图异常票券不可入场；迟到观众请听从现场工作人员安排。");
        card.setDetailBlocks(List.of(
                Map.of("type", "IMAGE", "content", card.getDetailImage()),
                Map.of("type", "HEADING", "content", "项目介绍"),
                Map.of("type", "PARAGRAPH", "content", card.getIntro()),
                Map.of("type", "HEADING", "content", "演职人员"),
                Map.of("type", "PARAGRAPH", "content", card.getArtistInfo()),
                Map.of("type", "HEADING", "content", "场馆介绍"),
                Map.of("type", "PARAGRAPH", "content", card.getVenueIntro())
        ));
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

    @SuppressWarnings("unchecked")
    private void applyPerformancePayload(PerformanceCard card, Map<String, Object> payload) {
        card.setTitle(str(payload, "title", "未命名演出"));
        card.setSubtitle(str(payload, "subtitle", str(payload, "summary", "请填写演出亮点")));
        card.setSummary(str(payload, "summary", card.getSubtitle()));
        card.setCategoryCode(str(payload, "categoryCode", "concert"));
        card.setCategoryName(str(payload, "categoryName", categoryName(card.getCategoryCode())));
        card.setVenueId(longValue(payload, "venueId", card.getVenueId()));
        card.setCity(str(payload, "city", "上海"));
        card.setVenue(str(payload, "venue", "待设置场馆"));
        card.setAddress(str(payload, "address", "待设置地址"));
        card.setStartTime(str(payload, "startTime", "2026-08-01 19:30"));
        card.setPriceMin(intValue(payload, "priceMin", 180));
        card.setPriceMax(intValue(payload, "priceMax", Math.max(card.getPriceMin(), 680)));
        card.setPoster(str(payload, "poster", "/uploads/posters/performance/poster-101.svg"));
        card.setBanner(str(payload, "banner", card.getPoster()));
        card.setDetailImage(str(payload, "detailImage", ""));
        card.setSaleStatus(str(payload, "saleStatus", "COMING_SOON"));
        card.setSaleMode(str(payload, "saleMode", "SELECTABLE"));
        card.setTags(stringList(payload.get("tags")));
        card.setIntro(str(payload, "intro", card.getSummary()));
        card.setArtistInfo(str(payload, "artistInfo", "演职人员信息待补充。"));
        card.setVenueIntro(str(payload, "venueIntro", card.getVenue() + "，" + card.getAddress()));
        card.setPurchaseNotice(str(payload, "purchaseNotice", "请确认场次、票档、数量和观演人信息后下单。"));
        card.setRefundRule(str(payload, "refundRule", "退票规则以本页面公示和订单规则为准。"));
        card.setEntryRule(str(payload, "entryRule", "请携带有效身份证件或电子票二维码入场。"));
        card.setPublishStatus(str(payload, "publishStatus", "DRAFT"));
        Object blocks = payload.get("detailBlocks");
        if (blocks instanceof List<?>) {
            card.setDetailBlocks(((List<?>) blocks).stream()
                    .filter(Map.class::isInstance)
                    .map(item -> Map.copyOf((Map<String, Object>) item))
                    .toList());
        } else {
            card.setDetailBlocks(List.of(
                    Map.of("type", "HEADING", "content", "项目介绍"),
                    Map.of("type", "PARAGRAPH", "content", card.getIntro())
            ));
        }
    }

    private String categoryName(String code) {
        return categories.stream()
                .filter(item -> Objects.equals(item.code(), code))
                .map(Category::name)
                .findFirst()
                .orElse("演出");
    }

    private void seedMovies() {
        movies.add(movie(201L, "星港来信", "剧情/奇幻", "2026-07-18", 118, "陆星遥", "林望舒、许澈", "8.6",
                "一名港口邮递员在旧灯塔中发现寄往未来的信件，由此牵出三位陌生人的夏夜约定。影片以温柔的城市夜景和轻奇幻设定讲述选择、告别与重新出发。"));
        movies.add(movie(202L, "云端列车", "冒险/家庭", "2026-07-26", 126, "沈雁南", "顾知远、叶清欢", "8.3",
                "少年工程师登上一列只在云层中运行的列车，寻找失踪的父亲和被遗忘的车站。影片将冒险、公路片节奏和家庭情感结合，适合全年龄观众。"));
        movies.add(movie(203L, "第七号观测站", "科幻/悬疑", "2026-08-01", 132, "韩青岚", "程砚、周栀", "8.8",
                "近未来的海岛观测站收到来自深空的重复信号，值守团队必须在风暴抵达前确认信号来源。影片以密闭空间和科学想象构建渐进式悬念。"));
        movies.add(movie(204L, "夏日猫咪事务所", "喜剧/治愈", "2026-08-09", 102, "林若川", "白禾、孟舟", "8.1",
                "一间只在夏天营业的街角事务所，专门帮邻里解决与猫有关的小麻烦。看似琐碎的委托逐渐拼成街区居民的温暖关系网。"));
        movies.add(movie(205L, "深海信号", "悬疑/冒险", "2026-08-16", 124, "叶知微", "秦砚、苏眠", "8.4",
                "海洋声学研究员在一次例行监听中捕捉到异常脉冲，她与队友潜入废弃海底站，寻找信号背后的真相。影片突出水下空间压迫感。"));
        movies.add(movie(206L, "城市微光", "剧情/音乐", "2026-08-23", 116, "江闻笛", "夏临、宋予白", "8.5",
                "三个在夜班中相遇的年轻人组成临时乐队，用一场街角演出回应各自的生活困境。影片以城市夜色、原创旋律和细腻群像铺陈希望。"));
    }

    private MovieCard movie(Long id, String title, String genre, String releaseDate, int duration, String director, String actors, String rating, String synopsis) {
        MovieCard movie = new MovieCard();
        movie.setId(id);
        movie.setTitle(title);
        movie.setGenre(genre);
        movie.setReleaseDate(releaseDate);
        movie.setDurationMinutes(duration);
        movie.setDirector(director);
        movie.setActors(actors);
        movie.setPoster("/uploads/posters/movie/movie-" + id + ".svg");
        movie.setSummary(synopsis);
        movie.setRating(rating);
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

    private String str(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private Long longValue(Map<String, Object> payload, String key, Long fallback) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        return Long.valueOf(String.valueOf(value));
    }

    private int intValue(Map<String, Object> payload, String key, int fallback) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        return Double.valueOf(String.valueOf(value)).intValue();
    }

    private List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .filter(item -> !item.isBlank())
                    .toList();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return List.of("电子票", "实名制");
        }
        return List.of(String.valueOf(value).split(",")).stream()
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
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
