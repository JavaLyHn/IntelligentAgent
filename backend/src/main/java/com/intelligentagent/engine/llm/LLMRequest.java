package com.intelligentagent.engine.llm;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class LLMRequest {

    private String provider;
    private String model;
    private String baseUrl;
    private String apiKey;
    private String systemPrompt;
    private String userPrompt;
    @Builder.Default
    private double temperature = 0.7;
    @Builder.Default
    private int maxTokens = 2000;
}
