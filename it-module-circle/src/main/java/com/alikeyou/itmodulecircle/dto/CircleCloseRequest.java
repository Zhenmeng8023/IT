package com.alikeyou.itmodulecircle.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "圈子关闭请求")
public class CircleCloseRequest {

    @Schema(description = "关闭原因", example = "违反社区规定", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String reason;

    @JsonIgnore
    @Schema(description = "操作人 ID", hidden = true)
    private Long operatorId;
}