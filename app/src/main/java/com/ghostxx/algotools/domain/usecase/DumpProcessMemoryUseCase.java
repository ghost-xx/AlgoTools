package com.ghostxx.algotools.domain.usecase;

import com.ghostxx.algotools.domain.entity.AppProcess;
import com.ghostxx.algotools.domain.entity.MemoryDump;
import com.ghostxx.algotools.domain.repository.MemoryDumpRepository;

/**
 * 内存转储用例
 */
public class DumpProcessMemoryUseCase {
    
    private final MemoryDumpRepository memoryDumpRepository;
    
    public DumpProcessMemoryUseCase(MemoryDumpRepository memoryDumpRepository) {
        this.memoryDumpRepository = memoryDumpRepository;
    }
    
    /**
     * 执行用例
     * @param process 要转储的进程
     * @return 内存转储结果，如果失败则返回null
     */
    public MemoryDump execute(AppProcess process) {
        if (!hasRequiredPermissions()) {
            return null;
        }
        
        if (memoryDumpRepository.needsCopyDumpTool()) {
            boolean copied = memoryDumpRepository.copyDumpTool();
            if (!copied) {
                return null;
            }
        }
        
        return memoryDumpRepository.dumpProcessMemory(process);
    }
    
    /**
     * 检查是否有所需权限
     * @return 如果有所有必要权限则返回true，否则返回false
     */
    public boolean hasRequiredPermissions() {
        return memoryDumpRepository.hasStoragePermission();
    }
    
    /**
     * 请求所需权限
     */
    public void requestPermissions() {
        memoryDumpRepository.requestStoragePermission();
    }
} 