package com.alikeyou.itmoduleai.provider.embedding;

import java.util.List;

public interface EmbeddingProvider {

    String providerCode();

    boolean supports(String providerCode);

    List<Double> embed(EmbeddingRequest request);
}
