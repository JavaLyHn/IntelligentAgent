package com.intelligentagent.engine.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class TTSNodeProcessor implements NodeProcessor {

    @Override
    public String getType() {
        return "toolNode";
    }

    @Override
    public Map<String, Object> process(Map<String, Object> inputData, Map<String, Object> config) {
        String text = getStr(inputData, "text", getStr(inputData, "input", ""));
        String voiceType = getStr(config, "voiceType", "female-1");
        double speed = getDouble(config, "speed", 1.0);
        int pitch = getInt(config, "pitch", 0);
        double volume = getDouble(config, "volume", 0.8);

        log.info("TTS Node: voiceType={}, speed={}, textLength={}", voiceType, speed, text.length());

        Map<String, Object> output = new HashMap<>();
        output.put("text", text);
        output.put("audioUrl", generateDemoAudioUrl(text, voiceType));
        output.put("voiceType", voiceType);
        output.put("duration", Math.ceil(text.length() / 4.0));
        output.put("format", "mp3");
        output.put("isDemo", true);

        return output;
    }

    private String generateDemoAudioUrl(String text, String voiceType) {
        return "data:audio/mp3;base64,SUQzBAAAAAAAI1RTU0UAAAAPAAADTGF2ZjU4Ljc2LjEwMAAAAAAAAAAAAAAA//tQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAWGluZwAAAA8AAAACAAABhgC7u7u7u7u7u7u7u7u7u7u7u7u7u7u7u7u7u7u7u7u7u7u7u7u7u7u7u7u7//////////////////////////////////////////////////////////////////8AAAAATGF2YzU4LjEzAAAAAAAAAAAAAAAAJAAAAAAAAAAAAYYAAAAAAAAAAAAAAAAAAAA//tQZAAP8AAAaQAAAAgAAA0gAAABAAABpAAAACAAADSAAAAETEFNRTMuMTAwVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVX/" + java.util.Base64.getEncoder().encodeToString(text.getBytes()).substring(0, Math.min(100, text.length()));
    }

    private String getStr(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultVal;
    }

    private double getDouble(Map<String, Object> map, String key, double defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        return defaultVal;
    }

    private int getInt(Map<String, Object> map, String key, int defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        return defaultVal;
    }
}
