package com.ghostxx.algotools.viewmodel;

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ghostxx.algotools.model.AnalysisResult;
import com.ghostxx.algotools.repository.HashRepository;
import com.ghostxx.algotools.utils.HashCryptoUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 哈希分析的ViewModel，负责处理与UI无关的业务逻辑
 */
public class HashAnalysisViewModel extends AndroidViewModel {
    private static final String TAG = "HashAnalysisViewModel";
    
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final AtomicBoolean isAnalyzing = new AtomicBoolean(false);
    private final HashRepository hashRepository;
    
    // LiveData对象
    private final MutableLiveData<AnalysisResult> analysisResult = new MutableLiveData<>();
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<Integer> progressPercent = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> lastFoundPlaintext = new MutableLiveData<>();
    
    public HashAnalysisViewModel(@NonNull Application application) {
        super(application);
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        hashRepository = new HashRepository(application);
        
        // 设置初始状态消息
        statusMessage.setValue("请输入哈希值并点击分析按钮\n\n支持: MD5, SHA-1, SHA-256, SHA-384, SHA-512");
    }
    
    /**
     * 启动哈希分析过程
     */
    @SuppressLint("DefaultLocale")
    public void analyzeHash(String hashToAnalyze, String featureString) {
        // 防止重复分析
        if (isAnalyzing.get()) {
            statusMessage.setValue("正在分析中，请稍候...");
            return;
        }
        
        // 输入验证
        if (hashToAnalyze == null || hashToAnalyze.trim().isEmpty()) {
            statusMessage.setValue("请输入哈希值");
            return;
        }
        
        // 处理哈希值
        hashToAnalyze = hashToAnalyze.trim().toLowerCase();
        
        // 识别哈希类型
        List<String> identifiedTypes = HashCryptoUtils.identifyHashType(hashToAnalyze);
        StringBuilder statusInfo = buildInitialStatus(identifiedTypes);
        
        if (!canCrackHash(hashToAnalyze, identifiedTypes, statusInfo)) {
            statusMessage.setValue(statusInfo.toString());
            return;
        }
        
        // 更新状态
        isAnalyzing.set(true);
        isLoading.setValue(true);
        progressPercent.setValue(0);
        
        String hashType = identifiedTypes.isEmpty() ? "MD5" : identifiedTypes.get(0);
        statusMessage.setValue(String.format("%s\n准备在内存中查找原文...\n哈希类型: %s", 
                                          statusInfo.toString(), hashType));
        
        // 执行分析
        String finalHashToAnalyze = hashToAnalyze;
        executorService.execute(() -> {
            try {
                File dumpFile = hashRepository.getDumpFile();
                if (dumpFile == null) {
                    throw new IllegalStateException("未找到内存转储文件或文件为空，请先转储");
                }
                
                // 开始搜索
                long startTime = System.currentTimeMillis();
                String result = hashRepository.searchPlaintext(
                    dumpFile,
                        finalHashToAnalyze,
                    featureString, 
                    identifiedTypes.isEmpty() ? "MD5" : identifiedTypes.get(0),
                    (current, total) -> {
                        int percent = (int) ((current * 100) / total);
                        // 避免过于频繁的更新，只在进度变化时更新
                        if (percent != progressPercent.getValue()) {
                            mainHandler.post(() -> progressPercent.setValue(percent));
                        }
                    }
                );
                
                long timeSpent = System.currentTimeMillis() - startTime;
                
                // 处理结果
                if (result != null && !result.isEmpty()) {
                    // 保存原文到单独的LiveData中，用于复制
                    lastFoundPlaintext.postValue(result);
                    
                    mainHandler.post(() -> {
                        // 设置分析结果
                        analysisResult.setValue(new AnalysisResult(true, result, timeSpent));
                        
                        // 设置状态消息，包含分析信息但不包含原文
                        statusMessage.setValue(String.format("哈希类型: %s\n↓↓↓↓↓↓↓↓\n %s\n处理用时: %.2f秒", 
                            identifiedTypes.isEmpty() ? "MD5" : identifiedTypes.get(0),
                            result,
                            timeSpent / 1000.0));
                    });
                } else {
                    mainHandler.post(() -> {
                        analysisResult.setValue(new AnalysisResult(false, null, timeSpent));
                        statusMessage.setValue(String.format("未找到匹配的原文。\n哈希类型: %s\n处理用时: %.2f秒", 
                            identifiedTypes.isEmpty() ? "MD5" : identifiedTypes.get(0),
                            timeSpent / 1000.0));
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    statusMessage.setValue("错误: " + e.getMessage());
                });
            } finally {
                isAnalyzing.set(false);
                mainHandler.post(() -> {
                    isLoading.setValue(false);
                    progressPercent.setValue(0);
                });
            }
        });
    }
    
    /**
     * 构建初始状态信息
     */
    private StringBuilder buildInitialStatus(List<String> identifiedTypes) {
        StringBuilder status = new StringBuilder();
        if (identifiedTypes.isEmpty()) {
            status.append("无法识别的哈希类型或无效哈希格式。\n");
        } else {
            status.append("当前CPU核心数: ").append(HashCryptoUtils.getAvailableProcessors()).append("\n");
            status.append("当前线程数: ").append(HashCryptoUtils.getThreadCount()).append("\n");
        }
        return status;
    }
    
    /**
     * 检查是否可以分析该哈希类型
     */
    private boolean canCrackHash(String hash, List<String> types, StringBuilder status) {
        boolean canCrackMD5 = types.contains("MD5") && hash.length() == 32;
        boolean canCrackSHA1 = types.contains("SHA-1") && hash.length() == 40;
        boolean canCrackSHA256 = types.contains("SHA-256") && hash.length() == 64;
        boolean canCrackSHA384 = types.contains("SHA-384") && hash.length() == 96;
        boolean canCrackSHA512 = types.contains("SHA-512") && hash.length() == 128;

        if (!canCrackMD5 && !canCrackSHA1 && !canCrackSHA256 && !canCrackSHA384 && !canCrackSHA512) {
            status.append(types.isEmpty() ? 
                "当前仅支持MD5, SHA-1, SHA-256, SHA-384和SHA-512原文查找。" :
                "\n注意: 当前仅支持MD5, SHA-1, SHA-256, SHA-384和SHA-512原文查找。");
            return false;
        }
        return true;
    }
    
    /**
     * 取消正在进行的分析
     */
    public void cancelAnalysis() {
        if (isAnalyzing.get()) {
            // 通知Repository取消操作
            hashRepository.cancelOperation();
            isAnalyzing.set(false);
            isLoading.postValue(false);
            statusMessage.postValue("分析已取消");
        }
    }
    
    // Getter方法，供Fragment观察
    public LiveData<AnalysisResult> getAnalysisResult() {
        return analysisResult;
    }
    
    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }
    
    public LiveData<Integer> getProgressPercent() {
        return progressPercent;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getLastFoundPlaintext() {
        return lastFoundPlaintext;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // 关闭线程池
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        // 取消任何进行中的操作
        hashRepository.cancelOperation();
    }
} 