package com.ghostxx.algotools.domain.repository;

import com.ghostxx.algotools.domain.entity.AppProcess;

/**
 * 进程仓库接口
 * 定义与进程相关的数据操作契约
 */
public interface ProcessRepository {
    
    /**
     * 获取前台应用进程
     * @return 前台应用进程信息，如果无法获取则返回null
     */
    AppProcess getForegroundProcess();
    
    /**
     * 根据包名获取应用进程
     * @param packageName 应用包名
     * @return 应用进程信息，如果未找到则返回null
     */
    AppProcess getProcessByPackageName(String packageName);
    
    /**
     * 检查是否有使用情况统计权限
     * @return 如果有权限则返回true，否则返回false
     */
    boolean hasUsageStatsPermission();
    
    /**
     * 请求使用情况统计权限
     */
    void requestUsageStatsPermission();
} 