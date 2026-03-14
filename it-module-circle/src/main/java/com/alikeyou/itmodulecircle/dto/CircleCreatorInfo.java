package com.alikeyou.itmodulecircle.dto;

import lombok.Data;

@Data
public class CircleCreatorInfo {
    private Long id;
    private String username;
    private String avatar;

    public CircleCreatorInfo() {
    }

    public CircleCreatorInfo(Long id, String username, String avatar) {
        this.id = id;
        this.username = username;
        this.avatar = avatar;
    }
}
