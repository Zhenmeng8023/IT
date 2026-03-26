package com.alikeyou.itmoduleai.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiCitationResponse {

    private Long knowledgeBaseId;
    private Long chunkId;
    private String chunkTitle;
}
