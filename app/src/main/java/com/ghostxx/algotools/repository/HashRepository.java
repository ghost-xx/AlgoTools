package com.ghostxx.algotools.repository;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ghostxx.algotools.repository.fileprocessing.FileProcessingEngine;
import com.ghostxx.algotools.repository.fileprocessing.HashSearchProcessor;

/**
 * 哈希分析仓库类，负责处理数据相关操作
 */
public class HashRepository {
    private static final String TAG = "HashRepository";
    
    private final Context context;
    private final FileProcessingEngine fileEngine;
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
    
    public HashRepository(Context context) {
        this.context = context.getApplicationContext(); // 使用应用程序上下文防止内存泄漏
        this.fileEngine = new FileProcessingEngine();
        
        // 配置文件处理引擎
        int cpuCores = Runtime.getRuntime().availableProcessors();
        int optimalThreads = Math.max(1, Math.min(cpuCores - 1, 4)); // 保留一个核心给UI线程
        
        fileEngine.setThreadCount(optimalThreads)
                  .setChunkSize(4 * 1024 * 1024) // 4MB
                  .setOverlapSize(128 * 1024); // 128KB
    }
    
    /**
     * 获取内存转储文件
     */
    public File getDumpFile() {
        File dumpFile = new File(context.getExternalFilesDir(null), "memory_data.bin");
        if (!dumpFile.exists() || dumpFile.length() == 0) {
            return null;
        }
        return dumpFile;
    }
    
    /**
     * 取消当前操作
     */
    public void cancelOperation() {
        cancelRequested.set(true);
        fileEngine.cancelOperation();
    }
    
    /**
     * 在内存转储文件中搜索哈希值对应的原文
     * @param dumpFile 内存转储文件
     * @param hashToCrack 要分析的哈希值
     * @param featureString 特征字符串（可选）
     * @param hashType 哈希类型
     * @param progressCallback 进度回调
     * @return 找到的原文，未找到则返回null
     */
    public String searchPlaintext(File dumpFile, String hashToCrack, String featureString, 
                                String hashType, ProgressCallback progressCallback) throws Exception {
        // 重置取消标志
        cancelRequested.set(false);
        
        long fileSize = dumpFile.length();
        Log.d(TAG, String.format("开始分析哈希值 %s (文件大小: %.2f MB)", 
                hashToCrack, fileSize / (1024.0 * 1024.0)));
        
        try {
            // 创建哈希搜索处理器
            HashSearchProcessor processor = new HashSearchProcessor(hashToCrack, featureString, hashType);
            
            // 创建进度回调转换器
            FileProcessingEngine.ProgressCallback engineCallback = null;
                if (progressCallback != null) {
                engineCallback = (current, total) -> {
                    progressCallback.onProgressUpdate(current, total);
                };
            }
            
            // 执行文件处理
            return fileEngine.processFile(dumpFile, processor, engineCallback);
            
        } catch (IOException e) {
            Log.e(TAG, "搜索哈希原文时出错: " + e.getMessage(), e);
            throw new Exception("处理文件时出错: " + e.getMessage(), e);
        }
    }
    
    /**
     * 进度回调接口
     */
    public interface ProgressCallback {
        void onProgressUpdate(long current, long total);
    }
} 