package com.alikeyou.itmoduleai.provider;

import com.alikeyou.itmoduleai.entity.AiModel;
import com.alikeyou.itmoduleai.provider.model.AiProviderChatRequest;
import com.alikeyou.itmoduleai.provider.model.AiProviderChatResponse;
import com.alikeyou.itmoduleai.provider.model.AiProviderStreamChunk;
import reactor.core.publisher.Flux;

public interface AiProvider {

    boolean supports(AiModel model);

    AiProviderChatResponse chat(AiProviderChatRequest request);

    Flux<AiProviderStreamChunk> streamChat(AiProviderChatRequest request);
}
