package com.alikeyou.itmodulecircle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "圈子更新请求")
public class CircleUpdateRequest {

    @Schema(description = "圈子名称", example = "技术交流圈", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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

    @Schema(description = "操作人 ID", example = "1", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long operatorId;
}