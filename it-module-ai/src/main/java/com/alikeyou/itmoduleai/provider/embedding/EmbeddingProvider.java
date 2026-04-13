package com.alikeyou.itmoduleai.provider.embedding;

import java.util.List;

public interface EmbeddingProvider {

    boolean supports(String providerCode);

    List<Double> embed(EmbeddingRequest request);
}
