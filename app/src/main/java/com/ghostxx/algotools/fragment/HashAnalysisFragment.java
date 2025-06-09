package com.ghostxx.algotools.fragment;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ghostxx.algotools.R;
import com.ghostxx.algotools.utils.CryptoUtils;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 哈希分析界面Fragment
 * 功能：
 * 1. 提供哈希值输入
 * 2. 提供特征字符串输入（可选，用于优化搜索）
 * 3. 分析哈希类型
 * 4. 在内存转储文件中搜索原文
 * 5. 显示分析结果和线程信息
 */
public class HashAnalysisFragment extends Fragment {
    private static final String TAG = "HashAnalysisFragment";
    private static final int CHUNK_SIZE = 8 * 1024 * 1024; // 8MB
    private static final int OVERLAP_SIZE = 128 * 1024;    // 128KB
    private static final int UPDATE_INTERVAL = 100;        // 进度更新间隔（毫秒）
    
    // 界面元素
    private TextInputEditText hashInput;          // 哈希值输入框
    private TextInputEditText featureInput;       // 特征字符串输入框
    private Button analyzeButton;                 // 分析按钮
    private TextView resultText;                  // 结果显示文本框
    private ImageButton copyPlaintextButtonNew;   // 新版复制按钮

    // 线程池和状态控制
    private ExecutorService executorService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean isAnalyzing = new AtomicBoolean(false);
    private String lastFoundPlaintext;           // 最后找到的原文
    private String currentHashType;              // 当前处理的哈希类型

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 创建单线程执行器，用于串行执行分析任务
        executorService = Executors.newSingleThreadExecutor();
    }

    @SuppressLint("SetTextI18x")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_hash_analysis, container, false);
        initializeViews(view);
        setInitialInstructions();
        return view;
    }
        
    private void initializeViews(View view) {
        // 绑定界面元素
        hashInput = view.findViewById(R.id.hashInput);
        featureInput = view.findViewById(R.id.featureInput);
        analyzeButton = view.findViewById(R.id.analyzeButton);
        resultText = view.findViewById(R.id.resultText);
        CheckBox jniLoggingCheckbox = view.findViewById(R.id.jniLoggingCheckbox);
        
        copyPlaintextButtonNew = view.findViewById(R.id.copyPlaintextButtonNew);
        // 旧版复制按钮（已隐藏）
        Button oldCopyPlaintextButton = view.findViewById(R.id.copyPlaintextButton);

        // 隐藏复制按钮
        copyPlaintextButtonNew.setVisibility(View.GONE);
        if (oldCopyPlaintextButton != null) {
            oldCopyPlaintextButton.setVisibility(View.GONE);
        }

        // 设置按钮事件
        analyzeButton.setOnClickListener(v -> startAnalysis());
        copyPlaintextButtonNew.setOnClickListener(v -> copyToClipboard());
        
        setupJniLogging(jniLoggingCheckbox);
    }

    private void copyToClipboard() {
        if (lastFoundPlaintext == null) return;
        
        Context context = getContext();
        if (context == null) return;

        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("原文", lastFoundPlaintext);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "原文已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }

    private void setupJniLogging(CheckBox checkbox) {
        checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            CryptoUtils.enableJniLogging(isChecked);
            String status = isChecked ? "已启用" : "已禁用";
            showToast("JNI 日志 " + status);
        });
        
        CryptoUtils.enableJniLogging(false);
        checkbox.setChecked(false);
    }

    @SuppressLint({"SetTextI18x", "SetTextI18n"})
    private void setInitialInstructions() {
        resultText.setText(
            "使用步骤：\n" +
            "1. 点击工具栏菜单中的\"启用悬浮窗\"\n" +
            "2. 切换到目标应用\n" +
            "3. 点击悬浮窗中的\"转储\"按钮获取内存数据\n" +
            "4. 输入哈希值 (支持MD5, SHA-1, SHA-256, SHA-384, SHA-512)\n" +
            "5. 可选：输入特征字符串以缩小搜索范围\n" +
            "6. 点击\"分析哈希并在内存中查找原文\"按钮"
        );
    }

    private void startAnalysis() {
        // 防止重复点击
        if (isAnalyzing.get()) {
            showToast("正在分析中，请稍候...");
            return;
        }

        // 获取并验证输入
        String hashToAnalyze = Objects.requireNonNull(hashInput.getText()).toString().trim().toLowerCase();
        String featureString = Objects.requireNonNull(featureInput.getText()).toString().trim();
        
        if (hashToAnalyze.isEmpty()) {
            showToast("请输入哈希值");
            return;
        }

        // 识别哈希类型
        List<String> identifiedTypes = CryptoUtils.identifyHashType(hashToAnalyze);
        StringBuilder statusInfo = buildInitialStatus(identifiedTypes);
        
        if (!canCrackHash(hashToAnalyze, identifiedTypes, statusInfo)) {
            resultText.setText(statusInfo.toString());
            return;
        }
        
        // 开始分析
        isAnalyzing.set(true);
        analyzeButton.setEnabled(false);
        copyPlaintextButtonNew.setVisibility(View.GONE);
        currentHashType = getHashType(identifiedTypes); // 设置当前哈希类型
        resultText.setText(String.format("%s准备在内存中查找原文...", statusInfo.toString()));
        
        searchPlaintext(hashToAnalyze, featureString, identifiedTypes, statusInfo);
    }

    private StringBuilder buildInitialStatus(List<String> identifiedTypes) {
        StringBuilder status = new StringBuilder();
        if (identifiedTypes.isEmpty()) {
            status.append("无法识别的哈希类型或无效哈希格式。\n");
        } else {
            status.append("当前CPU核心数: ").append(CryptoUtils.getAvailableProcessors()).append("\n");
            status.append("当前线程数: ").append(CryptoUtils.getThreadCount()).append("\n");
        }
        return status;
    }

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

    private void searchPlaintext(String hashToCrack, String featureString, 
                               List<String> identifiedTypes, StringBuilder statusInfo) {
        executorService.execute(() -> {
            try {
            Context context = getContext();
            if (context == null) {
                    throw new IllegalStateException("Context为空");
            }

            File dumpFile = new File(context.getExternalFilesDir(null), "memory_data.bin");
                if (!dumpFile.exists() || dumpFile.length() == 0) {
                    throw new IllegalStateException("未找到内存转储文件或文件为空，请先转储");
                }

                processMemoryDump(dumpFile, hashToCrack, featureString, identifiedTypes, statusInfo);

            } catch (Exception e) {
                showError(statusInfo.toString() + "错误: " + e.getMessage());
            } finally {
                isAnalyzing.set(false);
            }
        });
    }

    private void processMemoryDump(File dumpFile, String hashToCrack, String featureString,
                                 List<String> identifiedTypes, StringBuilder statusInfo) {
        long fileSize = dumpFile.length();
        long startTime = System.currentTimeMillis();

        // 对于大文件（>100MB），使用内存映射文件
        if (fileSize > 100 * 1024 * 1024) {
            processLargeFileWithMemoryMapping(dumpFile, fileSize, hashToCrack, featureString, 
                                             identifiedTypes, statusInfo, startTime, startTime);
        } else {
            // 对于小文件，使用传统的流式处理
            processSmallFileWithStream(dumpFile, fileSize, hashToCrack, featureString, 
                                      identifiedTypes, statusInfo, startTime, startTime);
        }
    }
    
    private void processLargeFileWithMemoryMapping(File dumpFile, long fileSize, String hashToCrack, 
                                                 String featureString, List<String> identifiedTypes, 
                                                 StringBuilder statusInfo, long startTime, long lastUpdateTime) {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(dumpFile, "r");
             FileChannel channel = randomAccessFile.getChannel()) {
            
            long position = 0;
            long mappingSize = Math.min(CHUNK_SIZE * 4, fileSize); // 每次映射最多4个块
            currentHashType = getHashType(identifiedTypes); // 设置当前哈希类型
            
            while (position < fileSize && !Thread.currentThread().isInterrupted()) {
                // 控制进度更新频率
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastUpdateTime >= UPDATE_INTERVAL) {
                    updateProgress(statusInfo, position, fileSize, identifiedTypes);
                    lastUpdateTime = currentTime;
                }
                
                // 计算当前映射的大小
                long currentSize = Math.min(mappingSize, fileSize - position);
                
                // 创建内存映射
                MappedByteBuffer buffer = channel.map(
                    FileChannel.MapMode.READ_ONLY, position, currentSize);
                
                // 将MappedByteBuffer转换为字节数组
                byte[] data = new byte[(int)currentSize];
                buffer.get(data);
                
                // 处理当前数据块
                String result = processDataChunk(hashToCrack, featureString, data, data.length, identifiedTypes);
                
                if (result != null) {
                    lastFoundPlaintext = result;
                    showSuccess(result, System.currentTimeMillis() - startTime);
                    return;
                }
                
                // 向前移动位置，确保有重叠区域
                position += (currentSize - OVERLAP_SIZE);
            }
            
            showNotFound(System.currentTimeMillis() - startTime);
            
        } catch (IOException e) {
            showError(statusInfo.toString() + "读取内存转储文件时出错: " + e.getMessage());
        }
    }
    
    private void processSmallFileWithStream(File dumpFile, long fileSize, String hashToCrack, 
                                          String featureString, List<String> identifiedTypes, 
                                          StringBuilder statusInfo, long startTime, long lastUpdateTime) {
        try (InputStream fis = Files.newInputStream(dumpFile.toPath())) {
            byte[] currentChunkPlusOverlap = new byte[CHUNK_SIZE + OVERLAP_SIZE];
            byte[] previousOverlap = new byte[OVERLAP_SIZE];
            Arrays.fill(previousOverlap, (byte)0);

            long currentFilePosition = 0;
            boolean isFirstChunk = true;
            currentHashType = getHashType(identifiedTypes); // 设置当前哈希类型

            while (currentFilePosition < fileSize && !Thread.currentThread().isInterrupted()) {
                // 控制进度更新频率
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastUpdateTime >= UPDATE_INTERVAL) {
                    updateProgress(statusInfo, currentFilePosition, fileSize, identifiedTypes);
                    lastUpdateTime = currentTime;
                }

                int bytesRead = readNextChunk(fis, currentChunkPlusOverlap, previousOverlap, 
                                           isFirstChunk, OVERLAP_SIZE);
                if (bytesRead <= 0) break;

                String result = processDataChunk(hashToCrack, featureString, currentChunkPlusOverlap, 
                                              bytesRead, identifiedTypes);
                
                if (result != null) {
                    lastFoundPlaintext = result;
                    showSuccess(result, System.currentTimeMillis() - startTime);
                    return;
                }

                updateOverlapAndPosition(currentChunkPlusOverlap, previousOverlap, bytesRead);
                currentFilePosition += (isFirstChunk ? CHUNK_SIZE : bytesRead);
                isFirstChunk = false;
            }

            showNotFound(System.currentTimeMillis() - startTime);

        } catch (IOException e) {
            showError(statusInfo.toString() + "读取内存转储文件时出错: " + e.getMessage());
        }
    }

    private int readNextChunk(InputStream fis, byte[] buffer, byte[] previousOverlap,
                            boolean isFirstChunk, int overlapSize) throws IOException {
        if (!isFirstChunk) {
            System.arraycopy(previousOverlap, 0, buffer, 0, overlapSize);
            return fis.read(buffer, overlapSize, buffer.length - overlapSize);
        }
        return fis.read(buffer);
    }

    private String processDataChunk(String hashToCrack, String featureString, byte[] data,
                                  int dataSize, List<String> identifiedTypes) {
        // 使用原生方法直接处理
        String hashType = getHashType(identifiedTypes);
        if (!hashType.equals("Unknown")) {
            // 创建包含实际数据的数组（去除可能的额外空间）
            byte[] chunkToProcess = Arrays.copyOfRange(data, 0, dataSize);
            
            // 使用原生方法查找原文
            return CryptoUtils.findOriginalTextNative(hashToCrack, chunkToProcess, hashType, featureString);
        }
        
        // 如果不是支持的哈希类型，返回null
        return null;
    }

    private void updateOverlapAndPosition(byte[] currentData, byte[] previousOverlap, int dataSize) {
        if (dataSize >= OVERLAP_SIZE) {
            System.arraycopy(currentData, dataSize - OVERLAP_SIZE, previousOverlap, 0, OVERLAP_SIZE);
                    } else {
            System.arraycopy(currentData, 0, previousOverlap, 0, dataSize);
            Arrays.fill(previousOverlap, dataSize, OVERLAP_SIZE, (byte)0);
        }
    }

    private void updateProgress(StringBuilder status, long current, long total, 
                              List<String> types) {
        String hashType = getHashType(types);
        long progressPercentage = (current * 100) / total;
        @SuppressLint("DefaultLocale") String progressMessage = String.format("%s正在分析 (%s)... %d%%",
            status, hashType, progressPercentage);
        
        mainHandler.post(() -> resultText.setText(progressMessage));
    }

    private String getHashType(List<String> types) {
        if (types.contains("MD5")) return "MD5";
        if (types.contains("SHA-1")) return "SHA-1";
        if (types.contains("SHA-256")) return "SHA-256";
        if (types.contains("SHA-384")) return "SHA-384";
        if (types.contains("SHA-512")) return "SHA-512";
        return "Unknown";
    }

    @SuppressLint("DefaultLocale")
    private void showSuccess(String plaintext, long timeSpent) {
        lastFoundPlaintext = plaintext;
        mainHandler.post(() -> {
            resultText.setText(String.format("找到原文！\n哈希类型: %s %s\n耗时: %.2f秒",
                currentHashType, plaintext, timeSpent / 1000.0));
            analyzeButton.setEnabled(true);
            copyPlaintextButtonNew.setVisibility(View.VISIBLE);
        });
    }

    @SuppressLint("DefaultLocale")
    private void showNotFound(long timeSpent) {
        mainHandler.post(() -> {
            resultText.setText(String.format("未找到原文\n哈希类型: %s\n耗时: %.2f秒", 
                currentHashType, timeSpent / 1000.0));
            analyzeButton.setEnabled(true);
        });
    }

    private void showError(String message) {
        Log.e(TAG, message);
        mainHandler.post(() -> {
            String errorMsg = message;
            if (currentHashType != null && !currentHashType.equals("Unknown")) {
                errorMsg = String.format("哈希类型: %s\n%s", currentHashType, message);
            }
            resultText.setText(errorMsg);
            analyzeButton.setEnabled(true);
        });
    }

    private void showToast(String message) {
        Context context = getContext();
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow(); // 立即中断所有任务
        }
        mainHandler.removeCallbacksAndMessages(null);
    }
} 