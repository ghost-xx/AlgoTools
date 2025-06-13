package com.ghostxx.algotools.common.di;

import android.content.Context;

import com.ghostxx.algotools.data.repository.HashAnalysisRepositoryImpl;
import com.ghostxx.algotools.data.repository.MemoryDumpRepositoryImpl;
import com.ghostxx.algotools.data.repository.ProcessRepositoryImpl;
import com.ghostxx.algotools.domain.repository.HashAnalysisRepository;
import com.ghostxx.algotools.domain.repository.MemoryDumpRepository;
import com.ghostxx.algotools.domain.repository.ProcessRepository;
import com.ghostxx.algotools.domain.usecase.AnalyzeHashUseCase;
import com.ghostxx.algotools.domain.usecase.DumpProcessMemoryUseCase;
import com.ghostxx.algotools.domain.usecase.GetForegroundProcessUseCase;

/**
 * 服务定位器
 * 提供应用程序各组件的单例实例
 */
public class ServiceLocator {
    
    private static ServiceLocator instance;
    
    private final Context applicationContext;
    
    // 仓库
    private ProcessRepository processRepository;
    private MemoryDumpRepository memoryDumpRepository;
    private HashAnalysisRepository hashAnalysisRepository;
    
    // 用例
    private GetForegroundProcessUseCase getForegroundProcessUseCase;
    private DumpProcessMemoryUseCase dumpProcessMemoryUseCase;
    private AnalyzeHashUseCase analyzeHashUseCase;
    
    private ServiceLocator(Context context) {
        this.applicationContext = context.getApplicationContext();
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized ServiceLocator getInstance(Context context) {
        if (instance == null) {
            instance = new ServiceLocator(context);
        }
        return instance;
    }
    
    /**
     * 获取进程仓库
     */
    public ProcessRepository getProcessRepository() {
        if (processRepository == null) {
            processRepository = new ProcessRepositoryImpl(applicationContext);
        }
        return processRepository;
    }
    
    /**
     * 获取内存转储仓库
     */
    public MemoryDumpRepository getMemoryDumpRepository() {
        if (memoryDumpRepository == null) {
            memoryDumpRepository = new MemoryDumpRepositoryImpl(applicationContext);
        }
        return memoryDumpRepository;
    }
    
    /**
     * 获取哈希分析仓库
     */
    public HashAnalysisRepository getHashAnalysisRepository() {
        if (hashAnalysisRepository == null) {
            hashAnalysisRepository = new HashAnalysisRepositoryImpl(applicationContext);
        }
        return hashAnalysisRepository;
    }
    
    /**
     * 获取前台进程用例
     */
    public GetForegroundProcessUseCase getGetForegroundProcessUseCase() {
        if (getForegroundProcessUseCase == null) {
            getForegroundProcessUseCase = new GetForegroundProcessUseCase(getProcessRepository());
        }
        return getForegroundProcessUseCase;
    }
    
    /**
     * 获取内存转储用例
     */
    public DumpProcessMemoryUseCase getDumpProcessMemoryUseCase() {
        if (dumpProcessMemoryUseCase == null) {
            dumpProcessMemoryUseCase = new DumpProcessMemoryUseCase(getMemoryDumpRepository());
        }
        return dumpProcessMemoryUseCase;
    }
    
    /**
     * 获取哈希分析用例
     */
    public AnalyzeHashUseCase getAnalyzeHashUseCase() {
        if (analyzeHashUseCase == null) {
            analyzeHashUseCase = new AnalyzeHashUseCase(
                    getHashAnalysisRepository(), 
                    getMemoryDumpRepository()
            );
        }
        return analyzeHashUseCase;
    }
    
    /**
     * 用于测试 - 设置进程仓库
     */
    public void setProcessRepository(ProcessRepository processRepository) {
        this.processRepository = processRepository;
    }
    
    /**
     * 用于测试 - 设置内存转储仓库
     */
    public void setMemoryDumpRepository(MemoryDumpRepository memoryDumpRepository) {
        this.memoryDumpRepository = memoryDumpRepository;
    }
    
    /**
     * 用于测试 - 设置哈希分析仓库
     */
    public void setHashAnalysisRepository(HashAnalysisRepository hashAnalysisRepository) {
        this.hashAnalysisRepository = hashAnalysisRepository;
    }
} 