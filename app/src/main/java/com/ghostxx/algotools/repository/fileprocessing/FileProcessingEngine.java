package com.ghostxx.algotools.repository.fileprocessing;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 文件处理引擎
 * 提供高效的文件读取和处理功能，支持多线程并行处理大文件
 */
public class FileProcessingEngine {
    private static final String TAG = "FileProcessingEngine";
    
    // 默认配置
    private static final int DEFAULT_CHUNK_SIZE = 4 * 1024 * 1024; // 4MB
    private static final int DEFAULT_OVERLAP_SIZE = 128 * 1024; // 128KB
    private static final int DEFAULT_THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int MAX_THREADS = 8; // 最大线程数限制
    
    // 可配置参数
    private int chunkSize = DEFAULT_CHUNK_SIZE;
    private int overlapSize = DEFAULT_OVERLAP_SIZE;
    private int threadCount = Math.min(DEFAULT_THREAD_COUNT, MAX_THREADS);
    
    // 运行时状态
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
    private final AtomicLong processedBytes = new AtomicLong(0);
    private final AtomicReference<String> result = new AtomicReference<>(null);
    private final ConcurrentHashMap<Integer, Boolean> chunkProcessed = new ConcurrentHashMap<>();
    
    /**
     * 设置块大小（字节）
     * @param chunkSize 块大小
     * @return this (链式调用)
     */
    public FileProcessingEngine setChunkSize(int chunkSize) {
        if (chunkSize > 0) {
            this.chunkSize = chunkSize;
        }
        return this;
    }
    
    /**
     * 设置重叠区域大小（字节）
     * @param overlapSize 重叠区域大小
     * @return this (链式调用)
     */
    public FileProcessingEngine setOverlapSize(int overlapSize) {
        if (overlapSize >= 0 && overlapSize < chunkSize) {
            this.overlapSize = overlapSize;
        }
        return this;
    }
    
    /**
     * 设置线程数
     * @param threadCount 线程数
     * @return this (链式调用)
     */
    public FileProcessingEngine setThreadCount(int threadCount) {
        if (threadCount > 0) {
            this.threadCount = Math.min(threadCount, MAX_THREADS);
        }
        return this;
    }
    
    /**
     * 请求取消操作
     */
    public void cancelOperation() {
        cancelRequested.set(true);
    }
    
    /**
     * 重置状态
     */
    private void resetState() {
        cancelRequested.set(false);
        processedBytes.set(0);
        result.set(null);
        chunkProcessed.clear();
    }
    
    /**
     * 处理文件
     * @param file 要处理的文件
     * @param processor 块处理器
     * @param progressCallback 进度回调
     * @return 处理结果，如果未找到则返回null
     * @throws IOException 如果文件处理出错
     */
    public String processFile(File file, FileChunkProcessor processor, ProgressCallback progressCallback) 
            throws IOException {
        resetState();
        
        long fileSize = file.length();
        Log.d(TAG, String.format("开始处理文件 %s (大小: %.2f MB), 使用 %d 个线程",
                file.getName(), fileSize / (1024.0 * 1024.0), threadCount));
        
        if (fileSize <= chunkSize * 2) {
            // 小文件使用单线程处理
            return processSmallFile(file, fileSize, processor, progressCallback);
        } else {
            // 大文件使用多线程处理
            return processLargeFile(file, fileSize, processor, progressCallback);
        }
    }
    
    /**
     * 处理小文件（单线程）
     */
    private String processSmallFile(File file, long fileSize, FileChunkProcessor processor,
                                 ProgressCallback progressCallback) throws IOException {
        Log.d(TAG, "使用单线程模式处理小文件");
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) Math.min(fileSize, chunkSize)];
            int bytesRead = fis.read(buffer);
            
            if (bytesRead > 0) {
                String result = processor.processChunk(buffer, bytesRead);
                if (result != null) {
                    return result;
                }
            }
            
            if (progressCallback != null) {
                progressCallback.onProgressUpdate(fileSize, fileSize);
            }
        }
        
        return null;
    }
    
    /**
     * 处理大文件（多线程）
     */
    private String processLargeFile(File file, long fileSize, FileChunkProcessor processor,
                                 ProgressCallback progressCallback) throws IOException {
        Log.d(TAG, "使用多线程模式处理大文件");
        
        // 计算总块数
        int totalChunks = (int) Math.ceil((double) fileSize / chunkSize);
        Log.d(TAG, "文件分为 " + totalChunks + " 个块进行处理");
        
        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<String>> futures = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(file);
             FileChannel channel = fis.getChannel()) {
            
            // 提交所有块处理任务
            for (int i = 0; i < totalChunks; i++) {
                final int chunkIndex = i;
                long position = (long) i * chunkSize;
                long remainingBytes = fileSize - position;
                int currentChunkSize = (int) Math.min(chunkSize, remainingBytes);
                
                // 为每个块创建任务
                Callable<String> task = () -> {
                    if (cancelRequested.get() || result.get() != null) {
                        return null; // 已经取消或找到结果
                    }
                    
                    try {
                        // 计算实际处理范围（包括重叠）
                        long actualPosition = Math.max(0, position - (chunkIndex > 0 ? overlapSize : 0));
                        long endPosition = Math.min(fileSize, position + currentChunkSize);
                        int actualSize = (int) (endPosition - actualPosition);
                        
                        // 映射当前块
                        MappedByteBuffer buffer = channel.map(
                                FileChannel.MapMode.READ_ONLY, actualPosition, actualSize);
                        
                        // 读取数据
                        byte[] data = new byte[actualSize];
                        buffer.get(data);
                        
                        // 处理数据
                        String chunkResult = processor.processChunk(data, actualSize);
                        
                        // 更新进度
                        long processed = processedBytes.addAndGet(currentChunkSize);
                        chunkProcessed.put(chunkIndex, true);
                        
                        if (progressCallback != null) {
                            progressCallback.onProgressUpdate(Math.min(processed, fileSize), fileSize);
                        }
                        
                        // 如果找到结果，设置到原子引用并返回
                        if (chunkResult != null) {
                            result.compareAndSet(null, chunkResult);
                            return chunkResult;
                        }
                        
                        // 在Java 10及以上版本可以使用 buffer.cleaner().clean() 清理映射缓冲区
                        // 但Android环境中我们依赖GC
                        buffer = null;
                        System.gc(); // 提示GC回收MappedByteBuffer
                        
                        return null;
                    } catch (Exception e) {
                        Log.e(TAG, "处理块 " + chunkIndex + " 时出错", e);
                        return null;
                    }
                };
                
                // 提交任务到线程池
                futures.add(executor.submit(task));
            }
            
            // 等待任何任务完成并返回结果
            String finalResult = null;
            for (Future<String> future : futures) {
                try {
                    String partialResult = future.get(100, TimeUnit.MILLISECONDS);
                    if (partialResult != null) {
                        finalResult = partialResult;
                        break;
                    }
                } catch (Exception e) {
                    // 超时或其他异常，继续检查下一个future
                }
                
                // 检查是否已有结果或已取消
                if (cancelRequested.get() || result.get() != null) {
                    break;
                }
            }
            
            // 如果还没有找到结果，等待所有任务完成
            if (finalResult == null && !cancelRequested.get()) {
                for (Future<String> future : futures) {
                    try {
                        String partialResult = future.get();
                        if (partialResult != null) {
                            finalResult = partialResult;
                            break;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "等待任务完成时出错", e);
                    }
                }
            }
            
            return finalResult != null ? finalResult : result.get();
            
        } finally {
            // 关闭线程池
            executor.shutdownNow();
            try {
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 进度回调接口
     */
    public interface ProgressCallback {
        void onProgressUpdate(long current, long total);
    }
} 