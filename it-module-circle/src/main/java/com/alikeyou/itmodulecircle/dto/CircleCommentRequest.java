package com.alikeyou.itmodulecircle.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CircleCommentRequest {
    private Long circleId;
    private Long authorId;
    private Long parentCommentId;
    private String content;
}
