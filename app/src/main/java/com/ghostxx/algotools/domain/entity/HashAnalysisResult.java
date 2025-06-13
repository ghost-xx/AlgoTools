package com.ghostxx.algotools.domain.entity;

/**
 * 哈希分析结果实体
 * 表示哈希分析操作的结果
 */
public class HashAnalysisResult {
    private final boolean isSuccess;
    private final String plaintext;
    private final String hashType;
    private final long timeSpentMs;
    
    private HashAnalysisResult(boolean isSuccess, String plaintext, String hashType, long timeSpentMs) {
        this.isSuccess = isSuccess;
        this.plaintext = plaintext;
        this.hashType = hashType;
        this.timeSpentMs = timeSpentMs;
    }
    
    /**
     * 分析是否成功
     */
    public boolean isSuccess() {
        return isSuccess;
    }
    
    /**
     * 获取找到的原文
     */
    public String getPlaintext() {
        return plaintext;
    }
    
    /**
     * 获取哈希类型
     */
    public String getHashType() {
        return hashType;
    }
    
    /**
     * 获取分析耗时（毫秒）
     */
    public long getTimeSpentMs() {
        return timeSpentMs;
    }
    
    /**
     * 获取分析耗时（秒）
     */
    public double getTimeSpentSeconds() {
        return timeSpentMs / 1000.0;
    }
    
    /**
     * 创建成功结果
     */
    public static HashAnalysisResult success(String plaintext, String hashType, long timeSpentMs) {
        return new HashAnalysisResult(true, plaintext, hashType, timeSpentMs);
    }
    
    /**
     * 创建失败结果
     */
    public static HashAnalysisResult failure(String hashType, long timeSpentMs) {
        return new HashAnalysisResult(false, null, hashType, timeSpentMs);
    }
} 