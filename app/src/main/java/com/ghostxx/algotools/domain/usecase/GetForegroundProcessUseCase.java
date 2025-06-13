package com.ghostxx.algotools.domain.usecase;

import com.ghostxx.algotools.domain.entity.AppProcess;
import com.ghostxx.algotools.domain.repository.ProcessRepository;

/**
 * 获取前台进程用例
 */
public class GetForegroundProcessUseCase {
    
    private final ProcessRepository processRepository;
    
    public GetForegroundProcessUseCase(ProcessRepository processRepository) {
        this.processRepository = processRepository;
    }
    
    /**
     * 执行用例
     * @return 前台应用进程
     */
    public AppProcess execute() {
        return processRepository.getForegroundProcess();
    }
    
    /**
     * 检查是否有使用情况统计权限
     * @return 如果有权限则返回true，否则返回false
     */
    public boolean hasRequiredPermissions() {
        return processRepository.hasUsageStatsPermission();
    }
    
    /**
     * 请求所需权限
     */
    public void requestPermissions() {
        processRepository.requestUsageStatsPermission();
    }
} 