package com.alikeyou.itmoduleinteractive.dto;

import lombok.Data;

@Data
public class NotificationPageRequest {

    private int page = 1;
    private int size = 20;
    private String category;
    private String type;
    private Boolean readStatus;
}
