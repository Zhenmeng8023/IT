package com.alikeyou.itmoduleblog.dto;

import lombok.Data;

@Data
public class AuthorInfo {
    private Long id;
    private String username;
    private String avatar;

    public AuthorInfo() {
    }

    public AuthorInfo(Long id, String username, String avatar) {
        this.id = id;
        this.username = username;
        this.avatar = avatar;
    }
}