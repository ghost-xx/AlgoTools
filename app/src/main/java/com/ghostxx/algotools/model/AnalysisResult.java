package com.ghostxx.algotools.model;

/**
 * 分析结果模型类
 */
public class AnalysisResult {
    private final boolean success;
    private final String plaintext;
    private final String errorMessage;
    private final long timeSpentMs;
    
    /**
     * 构造函数
     * @param success 是否成功
     * @param plaintext 明文（成功时有值）
     * @param timeSpentMs 处理时间（毫秒）
     */
    public AnalysisResult(boolean success, String plaintext, long timeSpentMs) {
        this.success = success;
        this.plaintext = plaintext;
        this.errorMessage = null;
        this.timeSpentMs = timeSpentMs;
    }
    
    /**
     * 构造函数
     * @param success 是否成功
     * @param plaintext 明文（成功时有值）
     * @param errorMessage 错误消息（失败时有值）
     */
    public AnalysisResult(boolean success, String plaintext, String errorMessage) {
        this.success = success;
        this.plaintext = plaintext;
        this.errorMessage = errorMessage;
        this.timeSpentMs = 0;
    }
    
    /**
     * 分析是否成功
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * 获取明文
     */
    public String getPlaintext() {
        return plaintext;
    }
    
    /**
     * 获取错误消息
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * 获取处理时间（毫秒）
     */
    public long getTimeSpentMs() {
        return timeSpentMs;
    }
    
    /**
     * 获取处理时间（秒）
     */
    public double getTimeSpentSeconds() {
        return timeSpentMs / 1000.0;
    }
} 