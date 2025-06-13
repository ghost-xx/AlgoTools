package com.ghostxx.algotools.domain.repository;

import com.ghostxx.algotools.domain.entity.HashAnalysisResult;
import com.ghostxx.algotools.domain.entity.MemoryDump;

/**
 * 哈希分析仓库接口
 * 定义与哈希分析相关的数据操作契约
 */
public interface HashAnalysisRepository {
    
    /**
     * 在内存转储中搜索哈希值对应的原文
     * @param dump 内存转储
     * @param hash 要分析的哈希值
     * @param featureString 特征字符串（可选，用于缩小搜索范围）
     * @param hashType 哈希类型
     * @param callback 进度回调
     * @return 哈希分析结果
     */
    HashAnalysisResult searchPlaintext(MemoryDump dump, String hash, String featureString, 
                                  String hashType, ProgressCallback callback);
    
    /**
     * 识别哈希类型
     * @param hash 哈希值
     * @return 可能的哈希类型列表
     */
    String[] identifyHashType(String hash);
    
    /**
     * 取消当前分析操作
     */
    void cancelAnalysis();
    
    /**
     * 进度回调接口
     */
    interface ProgressCallback {
        void onProgressUpdate(long current, long total);
    }
} 