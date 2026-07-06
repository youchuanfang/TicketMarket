package com.ticketmarket.service;

import com.ticketmarket.common.ApiException;
import com.ticketmarket.model.UserAccount;
import com.ticketmarket.model.Viewer;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@DependsOn("databaseSchemaInitializer")
public class Phase4TicketFlowService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DemoDataService demoDataService;
    private final Phase3ResourceService resourceService;
    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final AtomicLong orderId = new AtomicLong(5000);
    private final AtomicLong ticketId = new AtomicLong(8000);
    private final AtomicLong paymentId = new AtomicLong(9000);
    private final AtomicLong refundId = new AtomicLong(10000);
    private final AtomicLong checkinId = new AtomicLong(11000);
    private final AtomicLong messageId = new AtomicLong(12000);
    private final AtomicLong logId = new AtomicLong(13000);
    private final AtomicLong riskId = new AtomicLong(14000);
    private final AtomicLong reservationId = new AtomicLong(15000);
    private final Map<String, Map<String, Object>> rushRequests = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Object>> orders = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Object>> payments = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Object>> tickets = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Object>> refunds = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Object>> messages = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Object>> checkins = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Object>> operationLogs = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Object>> riskLogs = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Object>> blacklist = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Object>> reservations = new ConcurrentHashMap<>();

    public Phase4TicketFlowService(DemoDataService demoDataService, Phase3ResourceService resourceService, StringRedisTemplate redisTemplate, JdbcTemplate jdbcTemplate) {
        this.demoDataService = demoDataService;
        this.resourceService = resourceService;
        this.redisTemplate = redisTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initDemoRecords() {
        Integer persistedOrders = jdbcTemplate.queryForObject("select count(*) from ticket_order where deleted = 0", Integer.class);
        if (persistedOrders != null && persistedOrders > 0) {
            loadPersistentRecords();
            return;
        }
        Long id = orderId.incrementAndGet();
        Map<String, Object> order = map(
                "id", id,
                "orderNo", "TM-DEMO-" + id,
                "userId", 6L,
                "sessionId", 1001L,
                "batchId", 3001L,
                "ticketLevelId", 2001L,
                "ticketLevelName", "池座优选票",
                "quantity", 1,
                "viewerIds", List.of(2L),
                "selectedSeatIds", List.of(),
                "status", "TICKET_ISSUED",
                "totalAmount", new BigDecimal("380"),
                "expireTime", now(),
                "createdAt", now(),
                "updatedAt", now()
        );
        orders.put(id, order);
        persistOrder(order);
        Long sampleTicketId = ticketId.incrementAndGet();
        tickets.put(sampleTicketId, map(
                "id", sampleTicketId,
                "ticketNo", "ETDEMO" + sampleTicketId,
                "qrCodeContent", "TICKETMARKET:ETDEMO" + sampleTicketId,
                "orderId", id,
                "userId", 6L,
                "viewerId", 2L,
                "sessionId", 1001L,
                "ticketLevelId", 2001L,
                "seatId", null,
                "status", "UNUSED",
                "createdAt", now()
        ));
        persistTicket(tickets.get(sampleTicketId));
        refunds.put(refundId.incrementAndGet(), map(
                "id", refundId.get(),
                "orderId", id,
                "userId", 6L,
                "amount", new BigDecimal("380"),
                "feeRate", "0%",
                "status", "APPLYING",
                "message", "退票申请待审核",
                "createdAt", now(),
                "updatedAt", now()
        ));
        persistRefund(refunds.get(refundId.get()));
        checkins.put(checkinId.incrementAndGet(), map(
                "id", checkinId.get(),
                "ticketId", sampleTicketId,
                "ticketNo", "ETDEMO" + sampleTicketId,
                "checkerId", 3L,
                "result", "SUCCESS",
                "message", "核验成功",
                "createdAt", now()
        ));
        persistCheckin(checkins.get(checkinId.get()));
        addMessage(6L, "出票成功", "演示订单已出票，可在票夹查看。");
        log("演示数据", "初始化演示订单和电子票", 6L);
        risk("风控", "演示风控日志，可用于后台查看", null);
    }

    public synchronized Map<String, Object> submitRush(Long userId, Map<String, Object> payload) {
        UserAccount account = currentAccount(userId);
        if (!account.isRealNameVerified()) {
            return failedRush(userId, payload, "NO_AUTH", "请先完成实名认证");
        }
        Long sessionId = longValue(payload, "sessionId", null);
        Long ticketLevelId = longValue(payload, "ticketLevelId", null);
        Integer quantity = intValue(payload, "quantity", 1);
        if (sessionId == null || ticketLevelId == null || quantity == null || quantity < 1) {
            return failedRush(userId, payload, "FAILED", "请选择场次、票档和数量");
        }
        Map<String, Object> batch = resolveBatch(sessionId, longValue(payload, "batchId", null));
        try {
            resourceService.assertFrontSaleOpen(sessionId, (Long) batch.get("id"));
        } catch (ApiException ex) {
            return failedRush(userId, payload, "NOT_STARTED", "当前场次暂未开放购票，可先预约抢票");
        }
        String batchStatus = String.valueOf(batch.get("status"));
        if (!"SELLING".equals(batchStatus)) {
            return failedRush(userId, payload, "LOCKED", "本轮售票已锁票");
        }
        int purchaseLimit = intValue(batch, "purchaseLimit", 2);
        if (quantity > purchaseLimit) {
            return failedRush(userId, payload, "LIMITED", "已超出限购规则");
        }
        Map<String, Object> level = resourceService.ticketLevel(ticketLevelId);
        if (!Objects.equals(level.get("sessionId"), sessionId)) {
            return failedRush(userId, payload, "FAILED", "票档与场次不匹配，请重新选择");
        }
        List<Long> viewerIds = longList(payload.get("viewerIds"));
        if (viewerIds.size() != quantity) {
            return failedRush(userId, payload, "FAILED", "请选择对应数量的观演人");
        }
        List<Viewer> viewers = demoDataService.listViewers(userId);
        boolean invalidViewer = viewerIds.stream().anyMatch(id -> viewers.stream().noneMatch(viewer -> Objects.equals(viewer.getId(), id)));
        if (invalidViewer) {
            return failedRush(userId, payload, "FAILED", "观演人信息不可用，请重新选择");
        }
        if (hasDuplicateSuccess(userId, sessionId)) {
            return failedRush(userId, payload, "DUPLICATE", "检测到重复提交，请查看当前订单");
        }

        Long batchId = (Long) batch.get("id");
        List<Long> selectedSeatIds = longList(payload.get("selectedSeatIds"));
        List<Map<String, Object>> lockedSeats = List.of();
        try {
            if (!selectedSeatIds.isEmpty()) {
                if (selectedSeatIds.size() != quantity) {
                    return failedRush(userId, payload, "FAILED", "请选择对应数量的座位");
                }
                lockedSeats = resourceService.lockSessionSeats(sessionId, selectedSeatIds, userId, batchId, 5);
            } else {
                lockedSeats = resourceService.autoAllocateSeats(sessionId, ticketLevelId, quantity, userId, batchId);
                selectedSeatIds = lockedSeats.stream().map(item -> (Long) item.get("id")).toList();
            }
        } catch (ApiException ex) {
            return failedRush(userId, payload, "SOLD_OUT", ex.getMessage());
        }

        String stockKey = stockKey(batchId, ticketLevelId);
        ensureStock(batchId, stockKey);
        Long remain = redisTemplate.opsForValue().increment(stockKey, -quantity.longValue());
        if (remain == null || remain < 0) {
            redisTemplate.opsForValue().increment(stockKey, quantity.longValue());
            resourceService.releaseSessionSeats(selectedSeatIds, userId);
            return failedRush(userId, payload, "SOLD_OUT", "本轮票源已售罄");
        }

        Map<String, Object> order = createPendingOrder(userId, sessionId, batchId, ticketLevelId, quantity, viewerIds, selectedSeatIds);
        resourceService.increaseTicketLevelSold(ticketLevelId, 0);
        String requestId = "RQ" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        Map<String, Object> request = map(
                "requestId", requestId,
                "userId", userId,
                "sessionId", sessionId,
                "batchId", batchId,
                "ticketLevelId", ticketLevelId,
                "quantity", quantity,
                "status", "SUCCESS",
                "message", "抢票成功，请在 5 分钟内完成支付",
                "orderId", order.get("id"),
                "createdAt", now()
        );
        rushRequests.put(requestId, request);
        addMessage(userId, "抢票成功", "抢票成功，请在 5 分钟内完成支付。");
        log("抢票", "抢票成功，请求号：" + requestId, userId);
        return copy(request);
    }

    public Map<String, Object> rushRequest(String requestId, Long userId) {
        Map<String, Object> request = request(requestId);
        assertOwner(request, userId);
        return copy(request);
    }

    public Map<String, Object> rushResult(String requestId, Long userId) {
        Map<String, Object> request = rushRequest(requestId, userId);
        Long sessionId = longValue(request, "sessionId", null);
        if (sessionId != null) {
            request.put("performanceId", resourceService.session(sessionId).get("performanceId"));
        }
        Object orderIdValue = request.get("orderId");
        if (orderIdValue != null) {
            request.put("order", order((Long) orderIdValue, userId));
        }
        return request;
    }

    public Map<String, Object> createOrder(Long userId, Map<String, Object> payload) {
        Map<String, Object> request = submitRush(userId, payload);
        if (!"SUCCESS".equals(request.get("status"))) {
            throw new ApiException(409, String.valueOf(request.get("message")));
        }
        return order((Long) request.get("orderId"), userId);
    }

    public synchronized Map<String, Object> createReservation(Long userId, Map<String, Object> payload) {
        Long sessionId = longValue(payload, "sessionId", null);
        Long ticketLevelId = longValue(payload, "ticketLevelId", null);
        Integer quantity = intValue(payload, "quantity", 1);
        if (sessionId == null || ticketLevelId == null || quantity == null || quantity < 1) {
            throw new ApiException(400, "请选择场次、票档和数量");
        }
        Map<String, Object> level = resourceService.ticketLevel(ticketLevelId);
        if (!Objects.equals(level.get("sessionId"), sessionId)) {
            throw new ApiException(400, "票档与场次不匹配，请重新选择");
        }
        List<Long> viewerIds = longList(payload.get("viewerIds"));
        if (viewerIds.size() != quantity) {
            throw new ApiException(400, "请选择对应数量的观演人");
        }
        List<Viewer> viewers = demoDataService.listViewers(userId);
        boolean invalidViewer = viewerIds.stream().anyMatch(id -> viewers.stream().noneMatch(viewer -> Objects.equals(viewer.getId(), id)));
        if (invalidViewer) {
            throw new ApiException(400, "观演人信息不可用，请重新选择");
        }
        Map<String, Object> status = resourceService.frontSaleStatus(sessionId);
        if ("ON_SALE".equals(status.get("status"))) {
            throw new ApiException(409, "当前场次已开售，请直接购票");
        }
        Long id = reservationId.incrementAndGet();
        Map<String, Object> reservation = map(
                "id", id,
                "userId", userId,
                "sessionId", sessionId,
                "batchId", status.get("batchId"),
                "ticketLevelId", ticketLevelId,
                "ticketLevelName", level.get("name"),
                "quantity", quantity,
                "viewerIds", new ArrayList<>(viewerIds),
                "status", "RESERVED",
                "createdAt", now(),
                "updatedAt", now()
        );
        reservations.put(id, reservation);
        addMessage(userId, "预约抢票已提交", "开售前我们会保留你的预约信息，请在开售后回到项目页完成购票。");
        log("预约抢票", "用户提交预约提醒，场次：" + sessionId, userId);
        return copy(reservation);
    }

    public List<Map<String, Object>> userOrders(Long userId) {
        return orders.values().stream()
                .filter(item -> Objects.equals(item.get("userId"), userId))
                .sorted((a, b) -> String.valueOf(b.get("createdAt")).compareTo(String.valueOf(a.get("createdAt"))))
                .map(this::copy)
                .toList();
    }

    public List<Map<String, Object>> adminOrders() {
        return orders.values().stream().map(this::copy).toList();
    }

    public Map<String, Object> order(Long id, Long userId) {
        Map<String, Object> order = orders.get(id);
        if (order == null) {
            throw new ApiException(404, "订单不存在");
        }
        if (userId != null && !Objects.equals(order.get("userId"), userId)) {
            throw new ApiException(403, "无权限查看该订单");
        }
        return copy(order);
    }

    public synchronized Map<String, Object> cancelOrder(Long id, Long userId) {
        Map<String, Object> order = orders.get(id);
        if (order == null) {
            throw new ApiException(404, "订单不存在");
        }
        if (!Objects.equals(order.get("userId"), userId)) {
            throw new ApiException(403, "无权限操作该订单");
        }
        if (!"PENDING_PAYMENT".equals(order.get("status"))) {
            throw new ApiException(409, "当前订单不能取消");
        }
        order.put("status", "CANCELLED");
        order.put("updatedAt", now());
        updateOrder(order);
        returnStock(order);
        resourceService.releaseSessionSeats(longList(order.get("selectedSeatIds")), userId);
        return copy(order);
    }

    public synchronized Map<String, Object> pay(Long orderId, Long userId, Map<String, Object> payload) {
        Map<String, Object> order = orders.get(orderId);
        if (order == null) {
            throw new ApiException(404, "订单不存在");
        }
        if (!Objects.equals(order.get("userId"), userId)) {
            throw new ApiException(403, "无权限支付该订单");
        }
        if (!"PENDING_PAYMENT".equals(order.get("status"))) {
            throw new ApiException(409, "当前订单不能重复支付");
        }
        order.put("status", "PAID");
        order.put("paidAt", now());
        order.put("updatedAt", now());
        updateOrder(order);
        Map<String, Object> payment = map(
                "id", paymentId.incrementAndGet(),
                "orderId", orderId,
                "userId", userId,
                "payMethod", str(payload, "payMethod", "MOCK_ALIPAY"),
                "amount", order.get("totalAmount"),
                "status", "SUCCESS",
                "createdAt", now()
        );
        payments.put((Long) payment.get("id"), payment);
        persistPayment(payment);
        issueTickets(order);
        order.put("status", "TICKET_ISSUED");
        order.put("updatedAt", now());
        updateOrder(order);
        resourceService.markSessionSeatsSold(longList(order.get("selectedSeatIds")), userId);
        resourceService.increaseTicketLevelSold((Long) order.get("ticketLevelId"), intValue(order, "quantity", 1));
        addMessage(userId, "出票成功", "订单 " + order.get("orderNo") + " 已完成出票，可在票夹查看。");
        log("支付", "模拟支付成功，订单号：" + order.get("orderNo"), userId);
        return map("payment", copy(payment), "order", copy(order));
    }

    public Map<String, Object> payment(Long orderId, Long userId) {
        Map<String, Object> order = order(orderId, userId);
        return payments.values().stream()
                .filter(item -> Objects.equals(item.get("orderId"), order.get("id")))
                .findFirst()
                .map(this::copy)
                .orElse(map("orderId", orderId, "status", "WAITING_PAYMENT", "message", "订单待支付"));
    }

    public List<Map<String, Object>> userTickets(Long userId) {
        return tickets.values().stream()
                .filter(item -> Objects.equals(item.get("userId"), userId))
                .map(this::copy)
                .toList();
    }

    public List<Map<String, Object>> adminTickets() {
        return tickets.values().stream().map(this::copy).toList();
    }

    public Map<String, Object> ticket(Long id, Long userId) {
        Map<String, Object> ticket = tickets.get(id);
        if (ticket == null) {
            throw new ApiException(404, "电子票不存在");
        }
        if (userId != null && !Objects.equals(ticket.get("userId"), userId)) {
            throw new ApiException(403, "无权限查看该电子票");
        }
        return copy(ticket);
    }

    public synchronized Map<String, Object> applyRefund(Long orderIdValue, Long userId) {
        Map<String, Object> order = orders.get(orderIdValue);
        if (order == null) {
            throw new ApiException(404, "订单不存在");
        }
        if (!Objects.equals(order.get("userId"), userId)) {
            throw new ApiException(403, "无权限操作该订单");
        }
        if (!"TICKET_ISSUED".equals(order.get("status"))) {
            throw new ApiException(409, "当前订单暂不能申请退票");
        }
        boolean checkedIn = tickets.values().stream()
                .filter(item -> Objects.equals(item.get("orderId"), orderIdValue))
                .anyMatch(item -> "CHECKED_IN".equals(item.get("status")));
        if (checkedIn) {
            throw new ApiException(409, "已核验电子票不能退票");
        }
        Map<String, Object> refund = map(
                "id", refundId.incrementAndGet(),
                "orderId", orderIdValue,
                "userId", userId,
                "amount", order.get("totalAmount"),
                "feeRate", "0%",
                "status", "APPLYING",
                "message", "退票申请已提交，等待审核",
                "createdAt", now(),
                "updatedAt", now()
        );
        refunds.put((Long) refund.get("id"), refund);
        order.put("status", "REFUND_APPLYING");
        order.put("updatedAt", now());
        persistRefund(refund);
        updateOrder(order);
        addMessage(userId, "退票申请已提交", "订单 " + order.get("orderNo") + " 已提交退票申请，请等待审核。");
        log("退票", "用户提交退票申请，订单号：" + order.get("orderNo"), userId);
        return copy(refund);
    }

    public List<Map<String, Object>> userRefunds(Long userId) {
        return refunds.values().stream().filter(item -> Objects.equals(item.get("userId"), userId)).map(this::copy).toList();
    }

    public List<Map<String, Object>> adminRefunds() {
        return refunds.values().stream().map(this::copy).toList();
    }

    public synchronized Map<String, Object> approveRefund(Long id) {
        Map<String, Object> refund = refund(id);
        if (!"APPLYING".equals(refund.get("status"))) {
            throw new ApiException(409, "该退票申请已处理");
        }
        refund.put("status", "APPROVED");
        refund.put("message", "退票审核通过");
        refund.put("updatedAt", now());
        updateRefund(refund);
        Map<String, Object> order = orders.get((Long) refund.get("orderId"));
        order.put("status", "REFUNDED");
        order.put("updatedAt", now());
        updateOrder(order);
        tickets.values().stream()
                .filter(item -> Objects.equals(item.get("orderId"), order.get("id")))
                .forEach(item -> {
                    item.put("status", "REFUNDED");
                    updateTicket(item);
                });
        returnStock(order);
        addMessage((Long) order.get("userId"), "退票审核通过", "订单 " + order.get("orderNo") + " 已完成退票处理。");
        log("退票审核", "退票审核通过，订单号：" + order.get("orderNo"), (Long) order.get("userId"));
        return copy(refund);
    }

    public synchronized Map<String, Object> rejectRefund(Long id) {
        Map<String, Object> refund = refund(id);
        if (!"APPLYING".equals(refund.get("status"))) {
            throw new ApiException(409, "该退票申请已处理");
        }
        refund.put("status", "REJECTED");
        refund.put("message", "退票申请未通过");
        refund.put("updatedAt", now());
        updateRefund(refund);
        Map<String, Object> order = orders.get((Long) refund.get("orderId"));
        order.put("status", "TICKET_ISSUED");
        order.put("updatedAt", now());
        updateOrder(order);
        addMessage((Long) order.get("userId"), "退票申请未通过", "订单 " + order.get("orderNo") + " 暂不符合退票条件。");
        log("退票审核", "退票申请未通过，订单号：" + order.get("orderNo"), (Long) order.get("userId"));
        return copy(refund);
    }

    public synchronized Map<String, Object> verifyTicket(Map<String, Object> payload, Long checkerId) {
        String code = str(payload, "ticketNo", str(payload, "qrCodeContent", ""));
        Map<String, Object> ticket = tickets.values().stream()
                .filter(item -> Objects.equals(item.get("ticketNo"), code) || Objects.equals(item.get("qrCodeContent"), code))
                .findFirst()
                .orElse(null);
        if (ticket == null) {
            risk("检票", "无效票据：" + code, checkerId);
            return checkinResult(code, "INVALID", "无效票");
        }
        if ("REFUNDED".equals(ticket.get("status")) || "INVALID".equals(ticket.get("status"))) {
            return checkinResult(code, "INVALID", "票据已失效");
        }
        if ("CHECKED_IN".equals(ticket.get("status"))) {
            return checkinResult(code, "DUPLICATE", "重复入场");
        }
        ticket.put("status", "CHECKED_IN");
        updateTicket(ticket);
        Map<String, Object> order = orders.get((Long) ticket.get("orderId"));
        if (order != null) {
            order.put("status", "CHECKED_IN");
            order.put("updatedAt", now());
            updateOrder(order);
        }
        Map<String, Object> record = map(
                "id", checkinId.incrementAndGet(),
                "ticketId", ticket.get("id"),
                "ticketNo", ticket.get("ticketNo"),
                "checkerId", checkerId,
                "result", "SUCCESS",
                "message", "核验成功",
                "createdAt", now()
        );
        checkins.put((Long) record.get("id"), record);
        persistCheckin(record);
        addMessage((Long) ticket.get("userId"), "电子票已核验", "票号 " + ticket.get("ticketNo") + " 已完成入场核验。");
        log("检票", "核验成功，票号：" + ticket.get("ticketNo"), checkerId);
        return copy(record);
    }

    public List<Map<String, Object>> checkins() {
        return checkins.values().stream().map(this::copy).toList();
    }

    public List<Map<String, Object>> userMessages(Long userId) {
        return messages.values().stream().filter(item -> Objects.equals(item.get("userId"), userId)).map(this::copy).toList();
    }

    public List<Map<String, Object>> adminMessages() {
        return messages.values().stream().map(this::copy).toList();
    }

    public Map<String, Object> readMessage(Long id, Long userId) {
        Map<String, Object> message = messages.get(id);
        if (message == null || !Objects.equals(message.get("userId"), userId)) {
            throw new ApiException(404, "消息不存在");
        }
        message.put("read", true);
        return copy(message);
    }

    public Map<String, Object> announcement(Map<String, Object> payload) {
        String title = str(payload, "title", "系统消息");
        String content = str(payload, "content", "你有一条新的站内消息");
        orders.values().stream().map(item -> (Long) item.get("userId")).distinct().forEach(userId -> addMessage(userId, title, content));
        log("系统消息", "发布站内消息：" + title, null);
        return map("title", title, "content", content, "createdAt", now());
    }

    public Map<String, Object> statisticsOverview() {
        BigDecimal sales = orders.values().stream()
                .filter(item -> List.of("TICKET_ISSUED", "CHECKED_IN").contains(item.get("status")))
                .map(item -> new BigDecimal(String.valueOf(item.get("totalAmount"))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return map(
                "orderCount", orders.size(),
                "salesAmount", sales,
                "ticketCount", tickets.size(),
                "refundCount", refunds.size(),
                "checkinCount", checkins.size(),
                "rushSuccessRate", rushRequests.isEmpty() ? "0%" : successRate()
        );
    }

    public List<Map<String, Object>> operationLogs() {
        return operationLogs.values().stream().map(this::copy).toList();
    }

    public List<Map<String, Object>> riskLogs() {
        return riskLogs.values().stream().map(this::copy).toList();
    }

    public List<Map<String, Object>> blacklist() {
        return blacklist.values().stream().map(this::copy).toList();
    }

    public Map<String, Object> addBlacklist(Map<String, Object> payload) {
        Map<String, Object> row = map(
                "id", riskId.incrementAndGet(),
                "target", str(payload, "target", "未知对象"),
                "reason", str(payload, "reason", "异常访问"),
                "createdAt", now()
        );
        blacklist.put((Long) row.get("id"), row);
        risk("黑名单", "加入黑名单：" + row.get("target"), null);
        return copy(row);
    }

    public void removeBlacklist(Long id) {
        blacklist.remove(id);
    }

    public Map<String, Object> lockSeats(Long userId, Map<String, Object> payload) {
        Long sessionId = longValue(payload, "sessionId", null);
        List<Long> seatIds = longList(payload.get("seatIds"));
        if (sessionId == null || seatIds.isEmpty()) {
            throw new ApiException(400, "请选择要锁定的座位");
        }
        Map<String, Object> batch = resolveBatch(sessionId, longValue(payload, "batchId", null));
        resourceService.assertSeatSelectionOpen(sessionId, (Long) batch.get("id"));
        return map("seats", resourceService.lockSessionSeats(sessionId, seatIds, userId, (Long) batch.get("id"), 5), "lockExpireTime", FORMATTER.format(LocalDateTime.now().plusMinutes(5)));
    }

    public Map<String, Object> releaseSeats(Long userId, Map<String, Object> payload) {
        List<Long> seatIds = longList(payload.get("seatIds"));
        resourceService.releaseSessionSeats(seatIds, userId);
        return map("released", seatIds.size());
    }

    public Map<String, Object> locks(Long sessionId) {
        List<Map<String, Object>> locked = resourceService.sessionSeats(sessionId).stream()
                .filter(item -> "LOCKED".equals(item.get("status")))
                .toList();
        return map("sessionId", sessionId, "locks", locked);
    }

    private Map<String, Object> refund(Long id) {
        Map<String, Object> refund = refunds.get(id);
        if (refund == null) {
            throw new ApiException(404, "退票申请不存在");
        }
        return refund;
    }

    private void returnStock(Map<String, Object> order) {
        Map<String, Object> batch = resourceService.saleBatch((Long) order.get("batchId"));
        int quantity = intValue(order, "quantity", 1);
        if ("ON_SALE".equals(resourceService.frontSaleStatus((Long) order.get("sessionId")).get("status"))) {
            redisTemplate.opsForValue().increment(stockKey((Long) order.get("batchId"), (Long) order.get("ticketLevelId")), quantity);
        } else {
            resourceService.addStockPool(map(
                    "sessionId", order.get("sessionId"),
                    "ticketLevelId", order.get("ticketLevelId"),
                    "sourceType", "REFUND_RETURN",
                    "availableForNextBatch", true
            ));
        }
    }

    private Map<String, Object> checkinResult(String code, String result, String message) {
        Map<String, Object> record = map(
                "id", checkinId.incrementAndGet(),
                "ticketNo", code,
                "result", result,
                "message", message,
                "createdAt", now()
        );
        checkins.put((Long) record.get("id"), record);
        return copy(record);
    }

    private void addMessage(Long userId, String title, String content) {
        Map<String, Object> message = map(
                "id", messageId.incrementAndGet(),
                "userId", userId,
                "title", title,
                "content", content,
                "read", false,
                "createdAt", now()
        );
        messages.put((Long) message.get("id"), message);
    }

    private void log(String action, String detail, Long userId) {
        Map<String, Object> row = map(
                "id", logId.incrementAndGet(),
                "action", action,
                "detail", detail,
                "userId", userId,
                "createdAt", now()
        );
        operationLogs.put((Long) row.get("id"), row);
    }

    private void risk(String action, String detail, Long userId) {
        Map<String, Object> row = map(
                "id", riskId.incrementAndGet(),
                "action", action,
                "detail", detail,
                "userId", userId,
                "createdAt", now()
        );
        riskLogs.put((Long) row.get("id"), row);
    }

    private void loadPersistentRecords() {
        orders.clear();
        tickets.clear();
        refunds.clear();
        payments.clear();
        checkins.clear();
        jdbcTemplate.query("""
                select id, order_no orderNo, user_id userId, session_id sessionId, sale_batch_id batchId,
                       total_amount totalAmount, status, pay_deadline expireTime, paid_at paidAt,
                       date_format(created_at, '%Y-%m-%d %H:%i:%s') createdAt,
                       date_format(updated_at, '%Y-%m-%d %H:%i:%s') updatedAt
                from ticket_order where deleted = 0
                """, (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
            Long id = rs.getLong("id");
            List<Map<String, Object>> items = jdbcTemplate.queryForList("""
                    select ticket_level_id ticketLevelId, session_seat_id seatId, viewer_id viewerId, price
                    from order_item where order_id = ?
                    """, id);
            Long ticketLevelId = items.isEmpty() ? null : longValue(items.get(0), "ticketLevelId", null);
            Map<String, Object> level = ticketLevelId == null ? Map.of("name", "") : resourceService.ticketLevel(ticketLevelId);
            orders.put(id, map(
                    "id", id,
                    "orderNo", rs.getString("orderNo"),
                    "userId", rs.getLong("userId"),
                    "sessionId", rs.getLong("sessionId"),
                    "batchId", rs.getLong("batchId"),
                    "ticketLevelId", ticketLevelId,
                    "ticketLevelName", level.get("name"),
                    "quantity", items.size(),
                    "viewerIds", items.stream().map(item -> longValue(item, "viewerId", null)).filter(Objects::nonNull).toList(),
                    "selectedSeatIds", items.stream().map(item -> longValue(item, "seatId", null)).filter(Objects::nonNull).toList(),
                    "status", rs.getString("status"),
                    "totalAmount", rs.getBigDecimal("totalAmount"),
                    "expireTime", format(rs.getTimestamp("expireTime")),
                    "paidAt", format(rs.getTimestamp("paidAt")),
                    "createdAt", rs.getString("createdAt"),
                    "updatedAt", rs.getString("updatedAt")
            ));
        });
        jdbcTemplate.query("""
                select id, payment_no paymentNo, order_id orderId, user_id userId, channel payMethod, amount, status,
                       date_format(created_at, '%Y-%m-%d %H:%i:%s') createdAt
                from payment_record
                """, (org.springframework.jdbc.core.RowCallbackHandler) rs -> payments.put(rs.getLong("id"), map(
                "id", rs.getLong("id"),
                "paymentNo", rs.getString("paymentNo"),
                "orderId", rs.getLong("orderId"),
                "userId", rs.getLong("userId"),
                "payMethod", rs.getString("payMethod"),
                "amount", rs.getBigDecimal("amount"),
                "status", rs.getString("status"),
                "createdAt", rs.getString("createdAt")
        )));
        jdbcTemplate.query("""
                select id, ticket_no ticketNo, qr_payload qrCodeContent, order_id orderId, user_id userId,
                       viewer_id viewerId, session_id sessionId, ticket_level_id ticketLevelId,
                       session_seat_id seatId, status, date_format(created_at, '%Y-%m-%d %H:%i:%s') createdAt
                from e_ticket
                """, (org.springframework.jdbc.core.RowCallbackHandler) rs -> tickets.put(rs.getLong("id"), map(
                "id", rs.getLong("id"),
                "ticketNo", rs.getString("ticketNo"),
                "qrCodeContent", rs.getString("qrCodeContent"),
                "orderId", rs.getLong("orderId"),
                "userId", rs.getLong("userId"),
                "viewerId", nullableLong(rs.getObject("viewerId")),
                "sessionId", rs.getLong("sessionId"),
                "ticketLevelId", rs.getLong("ticketLevelId"),
                "seatId", nullableLong(rs.getObject("seatId")),
                "status", rs.getString("status"),
                "createdAt", rs.getString("createdAt")
        )));
        jdbcTemplate.query("""
                select ra.id, ra.order_id orderId, ra.user_id userId, rr.amount, ra.status, ra.reason message,
                       date_format(ra.created_at, '%Y-%m-%d %H:%i:%s') createdAt,
                       date_format(ra.updated_at, '%Y-%m-%d %H:%i:%s') updatedAt
                from refund_apply ra
                left join refund_record rr on rr.refund_apply_id = ra.id
                """, (org.springframework.jdbc.core.RowCallbackHandler) rs -> refunds.put(rs.getLong("id"), map(
                "id", rs.getLong("id"),
                "orderId", rs.getLong("orderId"),
                "userId", rs.getLong("userId"),
                "amount", rs.getBigDecimal("amount"),
                "feeRate", "0%",
                "status", rs.getString("status"),
                "message", rs.getString("message"),
                "createdAt", rs.getString("createdAt"),
                "updatedAt", rs.getString("updatedAt")
        )));
        jdbcTemplate.query("""
                select cr.id, cr.ticket_id ticketId, et.ticket_no ticketNo, cr.checker_id checkerId, cr.result, cr.message,
                       date_format(cr.created_at, '%Y-%m-%d %H:%i:%s') createdAt
                from checkin_record cr
                left join e_ticket et on et.id = cr.ticket_id
                """, (org.springframework.jdbc.core.RowCallbackHandler) rs -> checkins.put(rs.getLong("id"), map(
                "id", rs.getLong("id"),
                "ticketId", rs.getLong("ticketId"),
                "ticketNo", rs.getString("ticketNo"),
                "checkerId", rs.getLong("checkerId"),
                "result", rs.getString("result"),
                "message", rs.getString("message"),
                "createdAt", rs.getString("createdAt")
        )));
        resetIds();
    }

    private void persistOrder(Map<String, Object> order) {
        jdbcTemplate.update("""
                insert into ticket_order
                (id, order_no, user_id, session_id, sale_batch_id, total_amount, status, pay_deadline, paid_at, created_at, updated_at, deleted)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now(), 0)
                on duplicate key update status=values(status), total_amount=values(total_amount), pay_deadline=values(pay_deadline),
                                        paid_at=values(paid_at), updated_at=now()
                """,
                order.get("id"), order.get("orderNo"), order.get("userId"), order.get("sessionId"), order.get("batchId"),
                order.get("totalAmount"), order.get("status"), timestamp(order.get("expireTime")), timestamp(order.get("paidAt"))
        );
        jdbcTemplate.update("delete from order_item where order_id=?", order.get("id"));
        List<Long> viewerIds = longList(order.get("viewerIds"));
        List<Long> seatIds = longList(order.get("selectedSeatIds"));
        for (int i = 0; i < intValue(order, "quantity", viewerIds.size()); i++) {
            jdbcTemplate.update("""
                    insert into order_item (order_id, ticket_level_id, session_seat_id, viewer_id, price, created_at, updated_at)
                    values (?, ?, ?, ?, ?, now(), now())
                    """,
                    order.get("id"), order.get("ticketLevelId"), i < seatIds.size() ? seatIds.get(i) : null,
                    i < viewerIds.size() ? viewerIds.get(i) : null,
                    new BigDecimal(String.valueOf(order.get("totalAmount"))).divide(BigDecimal.valueOf(Math.max(1, intValue(order, "quantity", 1))))
            );
        }
    }

    private void updateOrder(Map<String, Object> order) {
        jdbcTemplate.update("""
                update ticket_order set status=?, paid_at=?, updated_at=now()
                where id=? and deleted=0
                """, order.get("status"), timestamp(order.get("paidAt")), order.get("id"));
    }

    private void persistPayment(Map<String, Object> payment) {
        jdbcTemplate.update("""
                insert into payment_record (id, payment_no, order_id, user_id, channel, amount, status, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, now(), now())
                on duplicate key update status=values(status), updated_at=now()
                """,
                payment.get("id"), "PAY" + payment.get("id"), payment.get("orderId"), payment.get("userId"),
                payment.get("payMethod"), payment.get("amount"), payment.get("status")
        );
    }

    private void persistTicket(Map<String, Object> ticket) {
        jdbcTemplate.update("""
                insert into e_ticket
                (id, ticket_no, order_id, order_item_id, user_id, viewer_id, session_id, ticket_level_id, session_seat_id, qr_payload, status, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())
                on duplicate key update status=values(status), updated_at=now()
                """,
                ticket.get("id"), ticket.get("ticketNo"), ticket.get("orderId"), ticket.get("id"), ticket.get("userId"),
                ticket.get("viewerId"), ticket.get("sessionId"), ticket.get("ticketLevelId"), ticket.get("seatId"),
                ticket.get("qrCodeContent"), ticket.get("status")
        );
    }

    private void updateTicket(Map<String, Object> ticket) {
        jdbcTemplate.update("update e_ticket set status=?, updated_at=now() where id=?", ticket.get("status"), ticket.get("id"));
    }

    private void persistRefund(Map<String, Object> refund) {
        jdbcTemplate.update("""
                insert into refund_apply (id, apply_no, order_id, user_id, reason, status, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, now(), now())
                on duplicate key update status=values(status), reason=values(reason), updated_at=now()
                """,
                refund.get("id"), "RF" + refund.get("id"), refund.get("orderId"), refund.get("userId"),
                refund.get("message"), refund.get("status")
        );
        Integer count = jdbcTemplate.queryForObject("select count(*) from refund_record where refund_apply_id=?", Integer.class, refund.get("id"));
        if (count == null || count == 0) {
            jdbcTemplate.update("""
                    insert into refund_record (refund_apply_id, order_id, amount, fee_amount, status, created_at, updated_at)
                    values (?, ?, ?, 0, ?, now(), now())
                    """, refund.get("id"), refund.get("orderId"), refund.get("amount"), refund.get("status"));
        } else {
            jdbcTemplate.update("update refund_record set status=?, updated_at=now() where refund_apply_id=?", refund.get("status"), refund.get("id"));
        }
    }

    private void updateRefund(Map<String, Object> refund) {
        jdbcTemplate.update("update refund_apply set status=?, reason=?, updated_at=now() where id=?", refund.get("status"), refund.get("message"), refund.get("id"));
        jdbcTemplate.update("update refund_record set status=?, updated_at=now() where refund_apply_id=?", refund.get("status"), refund.get("id"));
    }

    private void persistCheckin(Map<String, Object> record) {
        Object ticketIdValue = record.get("ticketId");
        if (ticketIdValue == null) return;
        Map<String, Object> ticket = tickets.get(Long.valueOf(String.valueOf(ticketIdValue)));
        jdbcTemplate.update("""
                insert into checkin_record (id, ticket_id, checker_id, session_id, result, message, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, now(), now())
                on duplicate key update result=values(result), message=values(message), updated_at=now()
                """,
                record.get("id"), ticketIdValue, record.get("checkerId"), ticket == null ? null : ticket.get("sessionId"),
                record.get("result"), record.get("message")
        );
    }

    private void resetIds() {
        orderId.set(maxId("ticket_order", 5000L));
        ticketId.set(maxId("e_ticket", 8000L));
        paymentId.set(maxId("payment_record", 9000L));
        refundId.set(maxId("refund_apply", 10000L));
        checkinId.set(maxId("checkin_record", 11000L));
    }

    private Long maxId(String table, Long fallback) {
        Long value = jdbcTemplate.queryForObject("select coalesce(max(id), ?) from " + table, Long.class, fallback);
        return value == null ? fallback : value;
    }

    private String successRate() {
        long success = rushRequests.values().stream().filter(item -> "SUCCESS".equals(item.get("status"))).count();
        return Math.round(success * 1000.0 / rushRequests.size()) / 10.0 + "%";
    }

    private Map<String, Object> createPendingOrder(Long userId, Long sessionId, Long batchId, Long ticketLevelId, int quantity, List<Long> viewerIds, List<Long> selectedSeatIds) {
        Map<String, Object> level = resourceService.ticketLevel(ticketLevelId);
        BigDecimal price = new BigDecimal(String.valueOf(level.get("price")));
        BigDecimal total = price.multiply(BigDecimal.valueOf(quantity));
        Long id = orderId.incrementAndGet();
        Map<String, Object> order = map(
                "id", id,
                "orderNo", "TM" + System.currentTimeMillis() + id,
                "userId", userId,
                "sessionId", sessionId,
                "batchId", batchId,
                "ticketLevelId", ticketLevelId,
                "ticketLevelName", level.get("name"),
                "quantity", quantity,
                "viewerIds", new ArrayList<>(viewerIds),
                "selectedSeatIds", new ArrayList<>(selectedSeatIds),
                "status", "PENDING_PAYMENT",
                "totalAmount", total,
                "expireTime", FORMATTER.format(LocalDateTime.now().plusMinutes(5)),
                "createdAt", now(),
                "updatedAt", now()
        );
        orders.put(id, order);
        persistOrder(order);
        return copy(order);
    }

    private void issueTickets(Map<String, Object> order) {
        Long orderIdValue = (Long) order.get("id");
        boolean alreadyIssued = tickets.values().stream().anyMatch(item -> Objects.equals(item.get("orderId"), orderIdValue));
        if (alreadyIssued) {
            return;
        }
        List<Long> viewerIds = longList(order.get("viewerIds"));
        List<Long> seatIds = longList(order.get("selectedSeatIds"));
        for (int i = 0; i < viewerIds.size(); i++) {
            Long id = ticketId.incrementAndGet();
            String ticketNo = "ET" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase();
            Map<String, Object> ticket = map(
                    "id", id,
                    "ticketNo", ticketNo,
                    "qrCodeContent", "TICKETMARKET:" + ticketNo,
                    "orderId", orderIdValue,
                    "userId", order.get("userId"),
                    "viewerId", viewerIds.get(i),
                    "sessionId", order.get("sessionId"),
                    "ticketLevelId", order.get("ticketLevelId"),
                    "seatId", i < seatIds.size() ? seatIds.get(i) : null,
                    "status", "UNUSED",
                    "createdAt", now()
            );
            tickets.put(id, ticket);
            persistTicket(ticket);
        }
    }

    private boolean hasDuplicateSuccess(Long userId, Long sessionId) {
        return orders.values().stream()
                .filter(item -> Objects.equals(item.get("userId"), userId))
                .filter(item -> Objects.equals(item.get("sessionId"), sessionId))
                .anyMatch(item -> List.of("PENDING_PAYMENT", "PAID", "TICKET_ISSUED").contains(item.get("status")));
    }

    private Map<String, Object> resolveBatch(Long sessionId, Long requestedBatchId) {
        Map<String, Object> batch = requestedBatchId == null ? resourceService.activeBatch(sessionId) : resourceService.saleBatch(requestedBatchId);
        if (batch == null || !Objects.equals(batch.get("sessionId"), sessionId)) {
            throw new ApiException(404, "当前场次暂无可用售票批次");
        }
        return batch;
    }

    private void ensureStock(Long batchId, String key) {
        if (redisTemplate.opsForValue().get(key) == null) {
            resourceService.initRedisStock(batchId);
        }
    }

    private Map<String, Object> failedRush(Long userId, Map<String, Object> payload, String status, String message) {
        String requestId = "RQ" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        Map<String, Object> request = map(
                "requestId", requestId,
                "userId", userId,
                "sessionId", longValue(payload, "sessionId", null),
                "batchId", longValue(payload, "batchId", null),
                "ticketLevelId", longValue(payload, "ticketLevelId", null),
                "quantity", intValue(payload, "quantity", 1),
                "status", status,
                "message", message,
                "createdAt", now()
        );
        rushRequests.put(requestId, request);
        return copy(request);
    }

    private Map<String, Object> request(String requestId) {
        Map<String, Object> request = rushRequests.get(requestId);
        if (request == null) {
            throw new ApiException(404, "抢票请求不存在");
        }
        return request;
    }

    private void assertOwner(Map<String, Object> row, Long userId) {
        if (!Objects.equals(row.get("userId"), userId)) {
            throw new ApiException(403, "无权限查看该记录");
        }
    }

    private UserAccount currentAccount(Long userId) {
        return demoDataService.findUserById(userId).orElseThrow(() -> new ApiException(401, "请先登录"));
    }

    private String stockKey(Long batchId, Long ticketLevelId) {
        return "ticket:batch:" + batchId + ":level:" + ticketLevelId + ":stock";
    }

    private String now() {
        return FORMATTER.format(LocalDateTime.now());
    }

    private String format(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : FORMATTER.format(timestamp.toLocalDateTime());
    }

    private java.sql.Timestamp timestamp(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return java.sql.Timestamp.valueOf(LocalDateTime.parse(String.valueOf(value), FORMATTER));
    }

    private Long nullableLong(Object value) {
        return value == null ? null : Long.valueOf(String.valueOf(value));
    }

    private Map<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }

    private Map<String, Object> map(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    private String str(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private Integer intValue(Map<String, Object> payload, String key, Integer fallback) {
        Object value = payload.get(key);
        if (value == null) {
            return fallback;
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private Long longValue(Map<String, Object> payload, String key, Long fallback) {
        Object value = payload.get(key);
        if (value == null) {
            return fallback;
        }
        return Long.valueOf(String.valueOf(value));
    }

    private List<Long> longList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().filter(Objects::nonNull).map(item -> Long.valueOf(String.valueOf(item))).toList();
    }
}
