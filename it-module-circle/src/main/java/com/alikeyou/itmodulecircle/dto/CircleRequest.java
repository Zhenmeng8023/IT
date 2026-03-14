package com.alikeyou.itmodulecircle.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CircleRequest {

    @NotBlank(message = "圈子名称不能为空")
    @Size(max = 100, message = "圈子名称不能超过 100 个字符")
    private String name;

    @Size(max = 1000, message = "描述不能超过 1000 个字符")
    private String description;

    private String type;

    private String visibility;

    @Min(value = 1, message = "最大成员数不能小于 1")
    @Max(value = 10000, message = "最大成员数不能超过 10000")
    private Integer maxMembers;

    private Long creatorId;

    private CircleCreatorInfo creatorInfo;
}
