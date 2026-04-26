package com.intelligentagent.engine.util;

import java.util.ArrayList;
import java.util.List;

public class TextSegmenter {

    private static final int MAX_SEGMENT_LENGTH = 600;

    private static final char[] SENTENCE_DELIMS = {'。', '！', '？', '；', '.', '!', '?', ';', '\n'};
    private static final char[] CLAUSE_DELIMS = {'，', '、', '：', ',', ':', ' '};

    public static List<String> segment(String text) {
        if (text == null || text.isEmpty()) return List.of();
        if (text.length() <= MAX_SEGMENT_LENGTH) return List.of(text);

        List<String> result = new ArrayList<>();
        String remaining = text;

        while (!remaining.isEmpty()) {
            if (remaining.length() <= MAX_SEGMENT_LENGTH) {
                String trimmed = remaining.trim();
                if (!trimmed.isEmpty()) result.add(trimmed);
                break;
            }

            int splitPos = findSplitPosition(remaining);
            String segment = remaining.substring(0, splitPos).trim();
            if (!segment.isEmpty()) {
                result.add(segment);
            }
            remaining = remaining.substring(splitPos).trim();
        }

        return result;
    }

    private static int findSplitPosition(String text) {
        int maxPos = Math.min(text.length(), MAX_SEGMENT_LENGTH);
        int minPos = Math.max(1, maxPos * 2 / 3);

        int pos = findLastDelimiter(text, SENTENCE_DELIMS, maxPos, minPos);
        if (pos > 0) return pos;

        pos = findLastDelimiter(text, CLAUSE_DELIMS, maxPos, minPos);
        if (pos > 0) return pos;

        return MAX_SEGMENT_LENGTH;
    }

    private static int findLastDelimiter(String text, char[] delimiters, int maxPos, int minPos) {
        for (int i = maxPos - 1; i >= minPos; i--) {
            char c = text.charAt(i);
            for (char delim : delimiters) {
                if (c == delim) return i + 1;
            }
        }
        return -1;
    }
}
