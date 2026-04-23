package com.intelligentagent.engine.llm;

public interface LLMProvider {

    String getName();

    LLMResponse chat(LLMRequest request);
}
