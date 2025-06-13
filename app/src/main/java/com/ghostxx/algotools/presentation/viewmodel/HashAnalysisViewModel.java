package com.ghostxx.algotools.presentation.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ghostxx.algotools.common.di.ServiceLocator;
import com.ghostxx.algotools.domain.entity.HashAnalysisResult;
import com.ghostxx.algotools.domain.entity.MemoryDump;
import com.ghostxx.algotools.domain.repository.HashAnalysisRepository;
import com.ghostxx.algotools.domain.repository.MemoryDumpRepository;
import com.ghostxx.algotools.domain.usecase.AnalyzeHashUseCase;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 哈希分析视图模型
 */
public class HashAnalysisViewModel extends AndroidViewModel {
    
    private final AnalyzeHashUseCase analyzeHashUseCase;
    private final MemoryDumpRepository memoryDumpRepository;
    private final ExecutorService executor;
    
    private final MutableLiveData<HashAnalysisResult> analysisResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isAnalyzing = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Integer> analysisProgress = new MutableLiveData<>(0);
    private final MutableLiveData<String[]> possibleHashTypes = new MutableLiveData<>();
    private final MutableLiveData<MemoryDump> currentDump = new MutableLiveData<>();
    
    public HashAnalysisViewModel(@NonNull Application application) {
        super(application);
        
        // 通过服务定位器获取用例
        ServiceLocator serviceLocator = ServiceLocator.getInstance(application);
        this.analyzeHashUseCase = serviceLocator.getAnalyzeHashUseCase();
        this.memoryDumpRepository = serviceLocator.getMemoryDumpRepository();
        
        // 创建线程池
        this.executor = Executors.newSingleThreadExecutor();
        
        // 获取当前转储
        loadCurrentDump();
    }
    
    /**
     * 加载当前转储
     */
    private void loadCurrentDump() {
        executor.execute(() -> {
            MemoryDump dump = memoryDumpRepository.getLatestDump();
            currentDump.postValue(dump);
        });
    }
    
    /**
     * 分析哈希
     * @param hash 要分析的哈希值
     * @param featureString 特征字符串（可选）
     */
    public void analyzeHash(String hash, String featureString) {
        if (hash == null || hash.isEmpty()) {
            errorMessage.setValue("请输入哈希值");
            return;
        }
        
        isAnalyzing.setValue(true);
        errorMessage.setValue(null);
        analysisProgress.setValue(0);
        
        // 识别哈希类型
        String[] types = analyzeHashUseCase.identifyHashType(hash);
        possibleHashTypes.setValue(types);
        
        // 创建进度回调
        HashAnalysisRepository.ProgressCallback callback = (current, total) -> {
            int progress = 0;
            if (total > 0) {
                progress = (int) ((current * 100) / total);
            }
            analysisProgress.postValue(progress);
        };
        
        executor.execute(() -> {
            try {
                HashAnalysisResult result = analyzeHashUseCase.execute(hash, featureString, callback);
                analysisResult.postValue(result);
                isAnalyzing.postValue(false);
                
                if (result != null && !result.isSuccess()) {
                    errorMessage.postValue("未找到匹配的原文");
                }
            } catch (Exception e) {
                isAnalyzing.postValue(false);
                errorMessage.postValue("分析失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 取消当前分析
     */
    public void cancelAnalysis() {
        if (isAnalyzing.getValue() != null && isAnalyzing.getValue()) {
            analyzeHashUseCase.cancel();
            isAnalyzing.setValue(false);
        }
    }
    
    /**
     * 识别哈希类型
     * @param hash 哈希值
     */
    public void identifyHashType(String hash) {
        if (hash == null || hash.isEmpty()) {
            possibleHashTypes.setValue(new String[0]);
            return;
        }
        
        executor.execute(() -> {
            String[] types = analyzeHashUseCase.identifyHashType(hash);
            possibleHashTypes.postValue(types);
        });
    }
    
    // LiveData访问器
    
    public LiveData<HashAnalysisResult> getAnalysisResult() {
        return analysisResult;
    }
    
    public LiveData<Boolean> isAnalyzing() {
        return isAnalyzing;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Integer> getAnalysisProgress() {
        return analysisProgress;
    }
    
    public LiveData<String[]> getPossibleHashTypes() {
        return possibleHashTypes;
    }
    
    public LiveData<MemoryDump> getCurrentDump() {
        return currentDump;
    }
    
    /**
     * 当前是否有可用的内存转储
     */
    public boolean hasDump() {
        return currentDump.getValue() != null && currentDump.getValue().isValid();
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        cancelAnalysis();
        executor.shutdown();
    }
} 