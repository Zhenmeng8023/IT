package com.alikeyou.itmodulepayment.dto;

import java.time.LocalDateTime;

public class MembershipDTO {
    private Long id;
    private Long userId;
    private Long levelId;
    private String levelName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Boolean isVip;

    public MembershipDTO() {
    }

    public MembershipDTO(Long id, Long userId, Long levelId, String levelName, 
                         LocalDateTime startTime, LocalDateTime endTime, 
                         String status, Boolean isVip) {
        this.id = id;
        this.userId = userId;
        this.levelId = levelId;
        this.levelName = levelName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.isVip = isVip;
    }

    // Getters and Setters
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

    public Long getLevelId() {
        return levelId;
    }

    public void setLevelId(Long levelId) {
        this.levelId = levelId;
    }

    public String getLevelName() {
        return levelName;
    }

    public void setLevelName(String levelName) {
        this.levelName = levelName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsVip() {
        return isVip;
    }

    public void setIsVip(Boolean isVip) {
        this.isVip = isVip;
    }
}
