package com.alikeyou.itmoduleai.dto.request;

import com.alikeyou.itmoduleai.entity.AiFeedbackLog;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiFeedbackCreateRequest {

    private Long callLogId;
    private Long messageId;
    private Long userId;
    private AiFeedbackLog.FeedbackType feedbackType;
    private String commentText;
}
