package com.ghostxx.algotools.repository.fileprocessing;

/**
 * 文件块处理器接口
 * 定义数据块处理的标准行为
 */
public interface FileChunkProcessor {
    /**
     * 处理数据块
     * @param data 数据字节数组
     * @param dataSize 有效数据大小
     * @return 处理结果，如果未找到结果则返回null
     */
    String processChunk(byte[] data, int dataSize);
    
    /**
     * 获取块处理器的名称（用于日志）
     * @return 处理器名称
     */
    String getName();
} 