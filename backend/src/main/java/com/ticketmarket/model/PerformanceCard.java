package com.ticketmarket.model;

import java.util.List;

public class PerformanceCard {
    private Long id;
    private String title;
    private String categoryCode;
    private String categoryName;
    private String city;
    private String venue;
    private String address;
    private String startTime;
    private Integer priceMin;
    private Integer priceMax;
    private String poster;
    private String saleStatus;
    private String saleMode;
    private List<String> tags;
    private String summary;
    private String intro;
    private String artistInfo;
    private String venueIntro;
    private String purchaseNotice;
    private String refundRule;
    private String entryRule;
    private List<SessionOption> sessions;
    private List<TicketLevel> ticketLevels;

    public PerformanceCard() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public Integer getPriceMin() {
        return priceMin;
    }

    public void setPriceMin(Integer priceMin) {
        this.priceMin = priceMin;
    }

    public Integer getPriceMax() {
        return priceMax;
    }

    public void setPriceMax(Integer priceMax) {
        this.priceMax = priceMax;
    }

    public String getPoster() {
        return poster;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }

    public String getSaleStatus() {
        return saleStatus;
    }

    public void setSaleStatus(String saleStatus) {
        this.saleStatus = saleStatus;
    }

    public String getSaleMode() {
        return saleMode;
    }

    public void setSaleMode(String saleMode) {
        this.saleMode = saleMode;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    public String getArtistInfo() {
        return artistInfo;
    }

    public void setArtistInfo(String artistInfo) {
        this.artistInfo = artistInfo;
    }

    public String getVenueIntro() {
        return venueIntro;
    }

    public void setVenueIntro(String venueIntro) {
        this.venueIntro = venueIntro;
    }

    public String getPurchaseNotice() {
        return purchaseNotice;
    }

    public void setPurchaseNotice(String purchaseNotice) {
        this.purchaseNotice = purchaseNotice;
    }

    public String getRefundRule() {
        return refundRule;
    }

    public void setRefundRule(String refundRule) {
        this.refundRule = refundRule;
    }

    public String getEntryRule() {
        return entryRule;
    }

    public void setEntryRule(String entryRule) {
        this.entryRule = entryRule;
    }

    public List<SessionOption> getSessions() {
        return sessions;
    }

    public void setSessions(List<SessionOption> sessions) {
        this.sessions = sessions;
    }

    public List<TicketLevel> getTicketLevels() {
        return ticketLevels;
    }

    public void setTicketLevels(List<TicketLevel> ticketLevels) {
        this.ticketLevels = ticketLevels;
    }
}
