package com.ticketmarket.model;

public class UserAccount {
    private Long id;
    private String username;
    private String passwordHash;
    private String nickname;
    private String roleCode;
    private boolean realNameVerified;

    public UserAccount() {
    }

    public UserAccount(Long id, String username, String passwordHash, String nickname, String roleCode, boolean realNameVerified) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.roleCode = roleCode;
        this.realNameVerified = realNameVerified;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public boolean isRealNameVerified() {
        return realNameVerified;
    }

    public void setRealNameVerified(boolean realNameVerified) {
        this.realNameVerified = realNameVerified;
    }
}
