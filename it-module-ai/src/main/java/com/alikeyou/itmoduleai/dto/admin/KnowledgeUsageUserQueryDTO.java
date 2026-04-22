package com.alikeyou.itmoduleai.dto.admin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeUsageUserQueryDTO {

    private String keyword;

    private Integer roleId;

    private String userStatus;

    private Boolean frozen;

    private Boolean importEnabled;

    private Boolean qaEnabled;
}
