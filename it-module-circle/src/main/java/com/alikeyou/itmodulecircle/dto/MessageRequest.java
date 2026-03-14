package com.alikeyou.itmodulecircle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageRequest {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 5000, message = "消息内容不能超过 5000 个字符")
    private String content;

    private String messageType;

    private Long receiverId;

    private Long circleId;
}
