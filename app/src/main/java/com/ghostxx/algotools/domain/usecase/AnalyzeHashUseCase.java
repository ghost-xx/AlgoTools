package com.ghostxx.algotools.domain.usecase;

import com.ghostxx.algotools.domain.entity.HashAnalysisResult;
import com.ghostxx.algotools.domain.entity.MemoryDump;
import com.ghostxx.algotools.domain.repository.HashAnalysisRepository;
import com.ghostxx.algotools.domain.repository.MemoryDumpRepository;

/**
 * 哈希分析用例
 */
public class AnalyzeHashUseCase {
    
    private final HashAnalysisRepository hashAnalysisRepository;
    private final MemoryDumpRepository memoryDumpRepository;
    
    public AnalyzeHashUseCase(HashAnalysisRepository hashAnalysisRepository, 
                            MemoryDumpRepository memoryDumpRepository) {
        this.hashAnalysisRepository = hashAnalysisRepository;
        this.memoryDumpRepository = memoryDumpRepository;
    }
    
    /**
     * 执行用例
     * @param hash 要分析的哈希值
     * @param featureString 特征字符串（可选）
     * @param callback 进度回调
     * @return 哈希分析结果
     */
    public HashAnalysisResult execute(String hash, String featureString, 
                                  HashAnalysisRepository.ProgressCallback callback) {
        // 获取最新的内存转储
        MemoryDump dump = memoryDumpRepository.getLatestDump();
        if (dump == null || !dump.isValid()) {
            return HashAnalysisResult.failure("未知", 0);
        }
        
        // 识别哈希类型
        String[] possibleTypes = hashAnalysisRepository.identifyHashType(hash);
        String hashType = possibleTypes.length > 0 ? possibleTypes[0] : "MD5";
        
        // 执行分析
        long startTime = System.currentTimeMillis();
        try {
            return hashAnalysisRepository.searchPlaintext(dump, hash, featureString, hashType, callback);
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            return HashAnalysisResult.failure(hashType, endTime - startTime);
        }
    }
    
    /**
     * 取消当前分析
     */
    public void cancel() {
        hashAnalysisRepository.cancelAnalysis();
    }
    
    /**
     * 识别哈希类型
     * @param hash 哈希值
     * @return 可能的哈希类型数组
     */
    public String[] identifyHashType(String hash) {
        return hashAnalysisRepository.identifyHashType(hash);
    }
} 