package com.ghostxx.algotools.domain.entity;

import java.util.Date;

/**
 * 内存转储实体
 * 表示从进程中提取的内存转储信息
 */
public class MemoryDump {
    private final String filePath;
    private final long size;
    private final Date creationTime;
    private final AppProcess sourceProcess;
    
    public MemoryDump(String filePath, long size, Date creationTime, AppProcess sourceProcess) {
        this.filePath = filePath;
        this.size = size;
        this.creationTime = creationTime;
        this.sourceProcess = sourceProcess;
    }
    
    /**
     * 获取转储文件路径
     */
    public String getFilePath() {
        return filePath;
    }
    
    /**
     * 获取转储文件大小（字节）
     */
    public long getSize() {
        return size;
    }
    
    /**
     * 获取转储创建时间
     */
    public Date getCreationTime() {
        return creationTime;
    }
    
    /**
     * 获取转储来源进程
     */
    public AppProcess getSourceProcess() {
        return sourceProcess;
    }
    
    /**
     * 检查转储文件是否有效
     */
    public boolean isValid() {
        return filePath != null && !filePath.isEmpty() && size > 0;
    }
} 