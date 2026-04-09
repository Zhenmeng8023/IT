package com.alikeyou.itmoduleproject.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectActivityPositionVO {
    private Long activityId;
    private Integer page;
    private Integer size;
    private Long total;
    private Boolean exists;
}
