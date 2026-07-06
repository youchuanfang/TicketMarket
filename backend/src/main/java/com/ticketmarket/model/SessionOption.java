package com.ticketmarket.model;

import java.math.BigDecimal;

public record SessionOption(Long id, String startTime, String saleStartTime, String lockTime, String hallName, String saleMode,
                            Long venueId, String city, String cinemaName, BigDecimal price, Integer stock) {
    public SessionOption(Long id, String startTime, String saleStartTime, String lockTime, String hallName, String saleMode) {
        this(id, startTime, saleStartTime, lockTime, hallName, saleMode, null, null, null, null, null);
    }
}
