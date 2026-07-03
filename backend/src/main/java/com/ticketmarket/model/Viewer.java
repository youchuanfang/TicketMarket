package com.ticketmarket.model;

public class Viewer {
    private Long id;
    private Long userId;
    private String name;
    private String idCardMasked;
    private String phoneMasked;
    private boolean defaultViewer;

    public Viewer() {
    }

    public Viewer(Long id, Long userId, String name, String idCardMasked, String phoneMasked) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.idCardMasked = idCardMasked;
        this.phoneMasked = phoneMasked;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdCardMasked() {
        return idCardMasked;
    }

    public void setIdCardMasked(String idCardMasked) {
        this.idCardMasked = idCardMasked;
    }

    public String getPhoneMasked() {
        return phoneMasked;
    }

    public void setPhoneMasked(String phoneMasked) {
        this.phoneMasked = phoneMasked;
    }

    public boolean isDefaultViewer() {
        return defaultViewer;
    }

    public void setDefaultViewer(boolean defaultViewer) {
        this.defaultViewer = defaultViewer;
    }
}
