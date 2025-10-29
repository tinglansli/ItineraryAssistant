package com.tinglans.backend.thirdparty.stt.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 文件处理工具类
 */
@Slf4j
public class FileUtil {

    /**
     * 小文件上传阈值：30MB
     */
    private static final long SMALL_FILE_THRESHOLD = 30 * 1024 * 1024;

    /**
     * 分块大小：10MB
     */
    private static final int CHUNK_SIZE = 10 * 1024 * 1024;

    /**
     * 判断是否为小文件
     */
    public static boolean isSmallFile(File file) {
        return file.length() <= SMALL_FILE_THRESHOLD;
    }

    /**
     * 读取文件内容
     */
    public static byte[] readFileBytes(File file) throws IOException {
        return FileUtils.readFileToByteArray(file);
    }

    /**
     * 将文件分块
     *
     * @param file 文件
     * @return 分块列表
     */
    public static List<FileChunk> splitFile(File file) throws IOException {
        List<FileChunk> chunks = new ArrayList<>();
        byte[] fileBytes = readFileBytes(file);
        int totalChunks = (int) Math.ceil((double) fileBytes.length / CHUNK_SIZE);

        for (int i = 0; i < totalChunks; i++) {
            int start = i * CHUNK_SIZE;
            int end = Math.min(start + CHUNK_SIZE, fileBytes.length);
            byte[] chunkData = Arrays.copyOfRange(fileBytes, start, end);

            chunks.add(new FileChunk(i, chunkData));
        }

        log.info("文件分块完成，总块数: {}, 文件大小: {} bytes", totalChunks, fileBytes.length);
        return chunks;
    }

    /**
     * 文件分块数据类
     */
    public static class FileChunk {
        private final int index;
        private final byte[] data;

        public FileChunk(int index, byte[] data) {
            this.index = index;
            this.data = data;
        }

        public int getIndex() {
            return index;
        }

        public byte[] getData() {
            return data;
        }
    }

    /**
     * 获取音频编码格式
     */
    public static String getEncoding(String fileName) {
        String lowerCaseName = fileName.toLowerCase();
        if (lowerCaseName.endsWith(".wav") || lowerCaseName.endsWith(".pcm")) {
            return "raw";
        } else if (lowerCaseName.endsWith(".mp3")) {
            return "lame";
        }
        throw new IllegalArgumentException("不支持的音频格式: " + fileName);
    }

    /**
     * 获取音频格式描述
     */
    public static String getFormat(String fileName) {
        // 默认返回16k采样率格式
        return "audio/L16;rate=16000";
    }
}
