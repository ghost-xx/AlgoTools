package com.ghostxx.algotools.domain.repository;

import com.ghostxx.algotools.domain.entity.AppProcess;
import com.ghostxx.algotools.domain.entity.MemoryDump;

/**
 * 内存转储仓库接口
 * 定义与内存转储相关的数据操作契约
 */
public interface MemoryDumpRepository {
    
    /**
     * 执行内存转储操作
     * @param process 要转储的进程
     * @return 转储结果，如果失败则返回null
     */
    MemoryDump dumpProcessMemory(AppProcess process);
    
    /**
     * 获取最新的内存转储
     * @return 最新的内存转储，如果不存在则返回null
     */
    MemoryDump getLatestDump();
    
    /**
     * 检查存储权限
     * @return 如果有权限则返回true，否则返回false
     */
    boolean hasStoragePermission();
    
    /**
     * 请求存储权限
     */
    void requestStoragePermission();
    
    /**
     * 检查是否需要复制转储工具
     * @return 如果需要复制则返回true，否则返回false
     */
    boolean needsCopyDumpTool();
    
    /**
     * 复制转储工具到设备
     * @return 复制是否成功
     */
    boolean copyDumpTool();
} 