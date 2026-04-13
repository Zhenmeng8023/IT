package com.alikeyou.itmoduleinteractive.dto;

import lombok.Data;

@Data
public class NotificationTemplateRequest {
    private String code;
    private String category;
    private String type;
    private String titleTemplate;
    private String contentTemplate;
    private String actionUrlTemplate;
    private Integer defaultPriority;
    private Boolean enabled;
    private String remark;
}
