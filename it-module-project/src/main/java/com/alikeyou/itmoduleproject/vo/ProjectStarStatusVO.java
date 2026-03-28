package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectStarStatusVO {
    private Long projectId;
    private Boolean starred;
    private Integer stars;
}
