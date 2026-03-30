package com.alikeyou.itmodulecircle.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "圈子创建请求")
public class CircleCreateRequest {

    @Schema(description = "圈子名称", example = "技术交流圈", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "圈子名称不能为空")
    @Size(max = 100, message = "圈子名称不能超过 100 个字符")
    private String name;

    @Schema(description = "圈子描述", example = "专注于技术交流和分享", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 1000, message = "描述不能超过 1000 个字符")
    private String description;

    @Schema(description = "可见性：public-公开，private-私有", example = "public", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String visibility;

    @Schema(description = "最大成员数", example = "500", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Min(value = 1, message = "最大成员数不能小于 1")
    @Max(value = 10000, message = "最大成员数不能超过 10000")
    private Integer maxMembers;

    @JsonIgnore
    @Schema(description = "创建者 ID", hidden = true)
    private Long creatorId;
}