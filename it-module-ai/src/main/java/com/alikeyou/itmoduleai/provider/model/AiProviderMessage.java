package com.alikeyou.itmoduleai.provider.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiProviderMessage {

    private String role;
    private String content;
}
