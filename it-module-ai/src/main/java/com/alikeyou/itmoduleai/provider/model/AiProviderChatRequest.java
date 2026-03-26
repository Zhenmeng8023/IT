package com.alikeyou.itmoduleai.provider.model;

import com.alikeyou.itmoduleai.entity.AiModel;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class AiProviderChatRequest {

    private AiModel model;
    private List<AiProviderMessage> messages;
    private Map<String, Object> requestParams;
}
