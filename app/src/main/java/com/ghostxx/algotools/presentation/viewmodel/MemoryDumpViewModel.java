package com.ghostxx.algotools.presentation.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ghostxx.algotools.common.di.ServiceLocator;
import com.ghostxx.algotools.domain.entity.AppProcess;
import com.ghostxx.algotools.domain.entity.MemoryDump;
import com.ghostxx.algotools.domain.usecase.DumpProcessMemoryUseCase;
import com.ghostxx.algotools.domain.usecase.GetForegroundProcessUseCase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 内存转储视图模型
 */
public class MemoryDumpViewModel extends AndroidViewModel {
    
    private final GetForegroundProcessUseCase getForegroundProcessUseCase;
    private final DumpProcessMemoryUseCase dumpProcessMemoryUseCase;
    private final ExecutorService executor;
    
    private final MutableLiveData<AppProcess> foregroundProcess = new MutableLiveData<>();
    private final MutableLiveData<MemoryDump> memoryDump = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    public MemoryDumpViewModel(@NonNull Application application) {
        super(application);
        
        // 通过服务定位器获取用例
        ServiceLocator serviceLocator = ServiceLocator.getInstance(application);
        this.getForegroundProcessUseCase = serviceLocator.getGetForegroundProcessUseCase();
        this.dumpProcessMemoryUseCase = serviceLocator.getDumpProcessMemoryUseCase();
        
        // 创建线程池
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * 获取前台进程
     */
    public void fetchForegroundProcess() {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        executor.execute(() -> {
            try {
                AppProcess process = getForegroundProcessUseCase.execute();
                foregroundProcess.postValue(process);
                isLoading.postValue(false);
                
                if (process == null) {
                    errorMessage.postValue("无法获取前台应用");
                }
            } catch (Exception e) {
                isLoading.postValue(false);
                errorMessage.postValue("获取前台应用失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 执行内存转储
     * @param process 要转储的进程
     */
    public void dumpProcessMemory(AppProcess process) {
        if (process == null || !process.isValid()) {
            errorMessage.setValue("无效的进程信息");
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        executor.execute(() -> {
            try {
                MemoryDump dump = dumpProcessMemoryUseCase.execute(process);
                memoryDump.postValue(dump);
                isLoading.postValue(false);
                
                if (dump == null) {
                    errorMessage.postValue("内存转储失败");
                }
            } catch (Exception e) {
                isLoading.postValue(false);
                errorMessage.postValue("内存转储失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 检查是否有使用情况统计权限
     */
    public boolean hasUsageStatsPermission() {
        return getForegroundProcessUseCase.hasRequiredPermissions();
    }
    
    /**
     * 请求使用情况统计权限
     */
    public void requestUsageStatsPermission() {
        getForegroundProcessUseCase.requestPermissions();
    }
    
    /**
     * 检查是否有存储权限
     */
    public boolean hasStoragePermission() {
        return dumpProcessMemoryUseCase.hasRequiredPermissions();
    }
    
    /**
     * 请求存储权限
     */
    public void requestStoragePermission() {
        dumpProcessMemoryUseCase.requestPermissions();
    }
    
    // LiveData访问器
    
    public LiveData<AppProcess> getForegroundProcess() {
        return foregroundProcess;
    }
    
    public LiveData<MemoryDump> getMemoryDump() {
        return memoryDump;
    }
    
    public LiveData<Boolean> isLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
} 