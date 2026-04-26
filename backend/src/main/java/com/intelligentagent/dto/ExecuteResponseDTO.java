package com.intelligentagent.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecuteResponseDTO {

    private boolean success;
    private String outputText;
    private String audioUrl;
    private String ttsStatus;
    private String ttsError;
    private List<String> logs;
    private Long durationMs;
    private String errorMessage;
}
