package com.ticketmarket.model;

import java.util.List;
import java.util.Map;

public class PerformanceCard {
    private Long id;
    private String title;
    private String subtitle;
    private String categoryCode;
    private String categoryName;
    private Long venueId;
    private String city;
    private String venue;
    private String address;
    private String startTime;
    private Integer priceMin;
    private Integer priceMax;
    private String poster;
    private String banner;
    private String detailImage;
    private String saleStatus;
    private String saleMode;
    private List<String> tags;
    private String summary;
    private String intro;
    private String detailContent;
    private String artistInfo;
    private String venueIntro;
    private String purchaseNotice;
    private String refundRule;
    private String refundFreeUntil;
    private String refundFeeUntil;
    private String refundStopTime;
    private String entryRule;
    private String publishStatus;
    private Boolean homeRecommended;
    private Integer homeSort;
    private List<Map<String, Object>> detailBlocks;
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

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
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

    public Long getVenueId() {
        return venueId;
    }

    public void setVenueId(Long venueId) {
        this.venueId = venueId;
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

    public String getBanner() {
        return banner;
    }

    public void setBanner(String banner) {
        this.banner = banner;
    }

    public String getDetailImage() {
        return detailImage;
    }

    public void setDetailImage(String detailImage) {
        this.detailImage = detailImage;
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

    public String getDetailContent() {
        return detailContent;
    }

    public void setDetailContent(String detailContent) {
        this.detailContent = detailContent;
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

    public String getRefundFreeUntil() {
        return refundFreeUntil;
    }

    public void setRefundFreeUntil(String refundFreeUntil) {
        this.refundFreeUntil = refundFreeUntil;
    }

    public String getRefundFeeUntil() {
        return refundFeeUntil;
    }

    public void setRefundFeeUntil(String refundFeeUntil) {
        this.refundFeeUntil = refundFeeUntil;
    }

    public String getRefundStopTime() {
        return refundStopTime;
    }

    public void setRefundStopTime(String refundStopTime) {
        this.refundStopTime = refundStopTime;
    }

    public String getEntryRule() {
        return entryRule;
    }

    public void setEntryRule(String entryRule) {
        this.entryRule = entryRule;
    }

    public String getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(String publishStatus) {
        this.publishStatus = publishStatus;
    }

    public Boolean getHomeRecommended() {
        return homeRecommended;
    }

    public void setHomeRecommended(Boolean homeRecommended) {
        this.homeRecommended = homeRecommended;
    }

    public Integer getHomeSort() {
        return homeSort;
    }

    public void setHomeSort(Integer homeSort) {
        this.homeSort = homeSort;
    }

    public List<Map<String, Object>> getDetailBlocks() {
        return detailBlocks;
    }

    public void setDetailBlocks(List<Map<String, Object>> detailBlocks) {
        this.detailBlocks = detailBlocks;
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
