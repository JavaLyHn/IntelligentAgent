package com.intelligentagent.engine.util;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

@Slf4j
public class AudioMerger {

    public static byte[] mergeAudioFiles(List<byte[]> audioDataList) {
        if (audioDataList == null || audioDataList.isEmpty()) {
            throw new IllegalArgumentException("No audio data to merge");
        }
        if (audioDataList.size() == 1) {
            return audioDataList.get(0);
        }

        boolean allWav = true;
        for (byte[] data : audioDataList) {
            if (!isWavFormat(data)) {
                allWav = false;
                break;
            }
        }

        if (allWav) {
            log.info("Merging {} WAV audio segments", audioDataList.size());
            return mergeWavFiles(audioDataList);
        } else {
            log.info("Merging {} audio segments (concatenation mode)", audioDataList.size());
            return concatenateAudio(audioDataList);
        }
    }

    public static boolean isWavFormat(byte[] data) {
        return data != null && data.length >= 12
                && new String(data, 0, 4).equals("RIFF")
                && new String(data, 8, 4).equals("WAVE");
    }

    public static String detectContentType(byte[] data) {
        if (isWavFormat(data)) return "audio/wav";
        if (data != null && data.length >= 3) {
            if ((data[0] & 0xFF) == 0xFF && ((data[1] & 0xFF) == 0xFB || (data[1] & 0xFF) == 0xF3 || (data[1] & 0xFF) == 0xF2)) {
                return "audio/mpeg";
            }
            if (data[0] == 0x49 && data[1] == 0x44 && data[2] == 0x33) {
                return "audio/mpeg";
            }
        }
        return "audio/wav";
    }

    private static byte[] mergeWavFiles(List<byte[]> wavFiles) {
        try {
            WavFormat firstFormat = null;
            ByteArrayOutputStream pcmStream = new ByteArrayOutputStream();

            for (int i = 0; i < wavFiles.size(); i++) {
                WavData wavData = parseWav(wavFiles.get(i));

                if (firstFormat == null) {
                    firstFormat = wavData.format;
                    log.info("WAV format: {}Hz, {}ch, {}bit, audioFormat={}",
                            firstFormat.sampleRate, firstFormat.channels,
                            firstFormat.bitsPerSample, firstFormat.audioFormat);
                } else {
                    if (wavData.format.sampleRate != firstFormat.sampleRate ||
                            wavData.format.channels != firstFormat.channels ||
                            wavData.format.bitsPerSample != firstFormat.bitsPerSample) {
                        log.warn("WAV format mismatch at segment {}: expected {}Hz/{}ch/{}bit, got {}Hz/{}ch/{}bit",
                                i, firstFormat.sampleRate, firstFormat.channels, firstFormat.bitsPerSample,
                                wavData.format.sampleRate, wavData.format.channels, wavData.format.bitsPerSample);
                    }
                }

                pcmStream.write(wavData.pcmData);
                log.info("WAV segment {}: {} bytes PCM data", i, wavData.pcmData.length);
            }

            byte[] mergedPcm = pcmStream.toByteArray();
            log.info("Total merged PCM data: {} bytes", mergedPcm.length);

            return buildWav(firstFormat, mergedPcm);
        } catch (Exception e) {
            log.error("WAV merge failed, falling back to concatenation: {}", e.getMessage());
            return concatenateAudio(wavFiles);
        }
    }

    private static byte[] concatenateAudio(List<byte[]> audioDataList) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            for (byte[] data : audioDataList) {
                stream.write(data);
            }
            return stream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("音频合并失败: " + e.getMessage(), e);
        }
    }

    private static WavData parseWav(byte[] wavData) {
        String riff = new String(wavData, 0, 4);
        if (!"RIFF".equals(riff)) throw new RuntimeException("Invalid WAV: missing RIFF header");
        String wave = new String(wavData, 8, 4);
        if (!"WAVE".equals(wave)) throw new RuntimeException("Invalid WAV: missing WAVE header");

        WavFormat format = null;
        byte[] pcmData = null;

        int pos = 12;
        while (pos < wavData.length - 8) {
            String chunkId = new String(wavData, pos, 4);
            int chunkSize = readLEInt(wavData, pos + 4);

            if ("fmt ".equals(chunkId)) {
                int audioFormat = readLEShort(wavData, pos + 8);
                int channels = readLEShort(wavData, pos + 10);
                int sampleRate = readLEInt(wavData, pos + 12);
                int bitsPerSample = readLEShort(wavData, pos + 22);
                format = new WavFormat(audioFormat, channels, sampleRate, bitsPerSample);
            } else if ("data".equals(chunkId)) {
                int dataSize = Math.min(chunkSize, wavData.length - pos - 8);
                pcmData = new byte[dataSize];
                System.arraycopy(wavData, pos + 8, pcmData, 0, dataSize);
                break;
            }

            pos += 8 + chunkSize;
            if (chunkSize % 2 != 0) pos++;
        }

        if (format == null) throw new RuntimeException("Invalid WAV: missing fmt chunk");
        if (pcmData == null) throw new RuntimeException("Invalid WAV: missing data chunk");

        return new WavData(format, pcmData);
    }

    private static byte[] buildWav(WavFormat format, byte[] pcmData) {
        int headerSize = 44;
        ByteBuffer buffer = ByteBuffer.allocate(headerSize + pcmData.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.put("RIFF".getBytes());
        buffer.putInt(36 + pcmData.length);
        buffer.put("WAVE".getBytes());

        buffer.put("fmt ".getBytes());
        buffer.putInt(16);
        buffer.putShort((short) format.audioFormat);
        buffer.putShort((short) format.channels);
        buffer.putInt(format.sampleRate);
        buffer.putInt(format.sampleRate * format.channels * format.bitsPerSample / 8);
        buffer.putShort((short) (format.channels * format.bitsPerSample / 8));
        buffer.putShort((short) format.bitsPerSample);

        buffer.put("data".getBytes());
        buffer.putInt(pcmData.length);
        buffer.put(pcmData);

        return buffer.array();
    }

    private static int readLEInt(byte[] data, int offset) {
        return (data[offset] & 0xFF) |
                ((data[offset + 1] & 0xFF) << 8) |
                ((data[offset + 2] & 0xFF) << 16) |
                ((data[offset + 3] & 0xFF) << 24);
    }

    private static int readLEShort(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }

    private static class WavFormat {
        final int audioFormat;
        final int channels;
        final int sampleRate;
        final int bitsPerSample;

        WavFormat(int audioFormat, int channels, int sampleRate, int bitsPerSample) {
            this.audioFormat = audioFormat;
            this.channels = channels;
            this.sampleRate = sampleRate;
            this.bitsPerSample = bitsPerSample;
        }
    }

    private static class WavData {
        final WavFormat format;
        final byte[] pcmData;

        WavData(WavFormat format, byte[] pcmData) {
            this.format = format;
            this.pcmData = pcmData;
        }
    }
}
