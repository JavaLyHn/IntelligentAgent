package com.intelligentagent.engine.llm;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class LLMResponse {

    private String text;
    private String model;
    private String provider;
    private int promptTokens;
    private int completionTokens;
    private boolean demo;
}
