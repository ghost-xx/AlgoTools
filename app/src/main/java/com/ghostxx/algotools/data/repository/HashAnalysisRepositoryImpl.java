package com.ghostxx.algotools.data.repository;

import android.content.Context;
import android.util.Log;

import com.ghostxx.algotools.domain.entity.HashAnalysisResult;
import com.ghostxx.algotools.domain.entity.MemoryDump;
import com.ghostxx.algotools.domain.repository.HashAnalysisRepository;
import com.ghostxx.algotools.repository.fileprocessing.FileProcessingEngine;
import com.ghostxx.algotools.repository.fileprocessing.HashSearchProcessor;
import com.ghostxx.algotools.utils.HashCryptoUtils;

import java.io.File;
import java.util.List;

/**
 * 哈希分析仓库实现
 */
public class HashAnalysisRepositoryImpl implements HashAnalysisRepository {
    
    private static final String TAG = "HashAnalysisRepoImpl";
    
    private final Context context;
    private final FileProcessingEngine fileEngine;
    
    public HashAnalysisRepositoryImpl(Context context) {
        this.context = context.getApplicationContext();
        
        // 创建并配置文件处理引擎
        this.fileEngine = new FileProcessingEngine();
        int cpuCores = Runtime.getRuntime().availableProcessors();
        int optimalThreads = Math.max(1, Math.min(cpuCores - 1, 4)); // 保留一个核心给UI线程
        
        fileEngine.setThreadCount(optimalThreads)
                 .setChunkSize(4 * 1024 * 1024) // 4MB
                 .setOverlapSize(128 * 1024); // 128KB
    }
    
    @Override
    public HashAnalysisResult searchPlaintext(MemoryDump dump, String hash, String featureString, 
                                       String hashType, ProgressCallback callback) {
        if (dump == null || !dump.isValid() || hash == null || hash.isEmpty()) {
            return HashAnalysisResult.failure(hashType, 0);
        }
        
        try {
            File dumpFile = new File(dump.getFilePath());
            if (!dumpFile.exists() || dumpFile.length() == 0) {
                Log.e(TAG, "转储文件不存在或为空");
                return HashAnalysisResult.failure(hashType, 0);
            }
            
            long startTime = System.currentTimeMillis();
            
            // 创建哈希搜索处理器
            HashSearchProcessor processor = new HashSearchProcessor(hash, featureString, hashType);
            
            // 创建进度回调转换器
            FileProcessingEngine.ProgressCallback engineCallback = null;
            if (callback != null) {
                engineCallback = (current, total) -> callback.onProgressUpdate(current, total);
            }
            
            // 执行文件处理
            String plaintext = fileEngine.processFile(dumpFile, processor, engineCallback);
            long endTime = System.currentTimeMillis();
            
            if (plaintext != null && !plaintext.isEmpty()) {
                return HashAnalysisResult.success(plaintext, hashType, endTime - startTime);
            } else {
                return HashAnalysisResult.failure(hashType, endTime - startTime);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "搜索哈希原文时出错", e);
            return HashAnalysisResult.failure(hashType, 0);
        }
    }
    
    @Override
    public String[] identifyHashType(String hash) {
        if (hash == null || hash.isEmpty()) {
            return new String[0];
        }
        
        List<String> identifiedTypes = HashCryptoUtils.identifyHashType(hash);
        return identifiedTypes.toArray(new String[0]);
    }
    
    @Override
    public void cancelAnalysis() {
        fileEngine.cancelOperation();
    }
} 