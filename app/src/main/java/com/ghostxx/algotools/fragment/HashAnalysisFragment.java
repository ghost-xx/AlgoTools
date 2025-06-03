package com.ghostxx.algotools.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HashAnalysisFragment extends Fragment {
    private static final String TAG = "HashAnalysisFragment";
    private TextInputEditText hashInput;
    private Button analyzeButton;
    private TextView resultText;
    private ImageButton copyPlaintextButtonNew;
    private Button oldCopyPlaintextButton;
    private ExecutorService executorService;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        executorService = Executors.newSingleThreadExecutor();
    }

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_hash_analysis, container, false);
        
        // 初始化视图
        hashInput = view.findViewById(R.id.hashInput);
        analyzeButton = view.findViewById(R.id.analyzeButton);
        resultText = view.findViewById(R.id.resultText);
        CheckBox jniLoggingCheckbox = view.findViewById(R.id.jniLoggingCheckbox);
        
        copyPlaintextButtonNew = view.findViewById(R.id.copyPlaintextButtonNew);
        oldCopyPlaintextButton = view.findViewById(R.id.copyPlaintextButton);

        copyPlaintextButtonNew.setVisibility(View.GONE);
        if (oldCopyPlaintextButton != null) {
            oldCopyPlaintextButton.setVisibility(View.GONE);
        }

        // 设置初始提示文本
        resultText.setText("使用步骤：\n" +
                "1. 点击工具栏菜单中的\"启用悬浮窗\"\n" +
                "2. 切换到目标应用\n" +
                "3. 点击悬浮窗中的\"转储\"按钮获取内存数据\n" +
                "4. 输入哈希值 (例如MD5, SHA-1, SHA-256)\n" +
                "5. 点击\"分析哈希并在内存中查找原文\"按钮");
        // 设置输入框提示

        analyzeButton.setText("分析哈希并在内存中查找原文");
        
        // 设置按钮点击事件
        analyzeButton.setOnClickListener(v -> startAnalysis());
        
        // 设置JNI日志复选框的监听器
        jniLoggingCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            CryptoUtils.enableJniLogging(isChecked);
            String status = isChecked ? "已启用" : "已禁用";
            Toast.makeText(getContext(), "JNI 日志 " + status, Toast.LENGTH_SHORT).show();
        });
        
        // 初始状态下禁用JNI日志
        CryptoUtils.enableJniLogging(false);
        jniLoggingCheckbox.setChecked(false);

        return view;
    }

    @SuppressLint("SetTextI18n")
    private void startAnalysis() {
        String hashToAnalyze = Objects.requireNonNull(hashInput.getText()).toString().trim().toLowerCase();
        if (hashToAnalyze.isEmpty()) {
            Toast.makeText(getContext(), "请输入哈希值", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> identifiedTypes = CryptoUtils.identifyHashType(hashToAnalyze);
        StringBuilder identifiedTypesText = new StringBuilder();
        if (identifiedTypes.isEmpty()) {
            identifiedTypesText.append("无法识别的哈希类型或无效哈希格式。\n");
        } else {
            identifiedTypesText.append("识别出的哈希类型: ").append(identifiedTypes.toString()).append("\n");
        }

        analyzeButton.setEnabled(false);

        boolean canCrackMD5 = identifiedTypes.contains("MD5") && hashToAnalyze.length() == 32;
        boolean canCrackSHA1 = identifiedTypes.contains("SHA-1") && hashToAnalyze.length() == 40;
        boolean canCrackSHA256 = identifiedTypes.contains("SHA-256") && hashToAnalyze.length() == 64;

        if (!canCrackMD5 && !canCrackSHA1 && !canCrackSHA256) {
            String finalMessage = identifiedTypesText.toString();
            if (!identifiedTypes.isEmpty()) {
                 finalMessage += "\n注意: 当前仅支持MD5, SHA-1和SHA-256原文查找。";
            } else {
                finalMessage += "当前仅支持MD5, SHA-1和SHA-256原文查找。";
            }
            resultText.setText(finalMessage);
            analyzeButton.setEnabled(true);
            return;
        }
        
        resultText.setText(identifiedTypesText.toString() + "准备在内存中查找原文...");

        final String hashToCrack = hashToAnalyze;
        final String crackType;
        if (canCrackMD5) {
            crackType = "MD5";
        } else if (canCrackSHA1) {
            crackType = "SHA-1";
        } else if (canCrackSHA256) {
            crackType = "SHA-256";
        } else {
            crackType = "Unknown";
        }

        executorService.execute(() -> {
            String foundPlaintext = null;
            Context context = getContext();
            if (context == null) {
                Log.e(TAG, "Context is null, cannot proceed with analysis.");
                showError(identifiedTypesText.toString() + "内部错误: Context为空");
                return;
            }

            File dumpFile = new File(context.getExternalFilesDir(null), "memory_data.bin");
            if (!dumpFile.exists()) {
                showError(identifiedTypesText.toString() + "未找到内存转储文件，请先转储");
                return;
            }

            final int CHUNK_SIZE = 4 * 1024 * 1024; // 4MB
            final int OVERLAP_SIZE = 128 * 1024; // 128KB
            long fileSize = dumpFile.length();

            if (fileSize == 0) {
                showError(identifiedTypesText.toString() + "内存转储文件为空");
                return;
            }
            
            long startTime = System.currentTimeMillis();

            try (InputStream fis = new FileInputStream(dumpFile)) {
                byte[] currentChunkPlusOverlap = new byte[CHUNK_SIZE + OVERLAP_SIZE];
                byte[] previousOverlap = new byte[OVERLAP_SIZE];
                Arrays.fill(previousOverlap, (byte)0);

                byte[] dataToProcess;
                int bytesActuallyRead;
                boolean isFirstChunk = true;
                long currentFilePosition = 0;

                while(currentFilePosition < fileSize){
                    if (getActivity() != null) {
                        long progressPercentage = (currentFilePosition * 100) / fileSize;
                        final String progressMessage = identifiedTypesText.toString() + "正在分析 ("+ crackType +")... " + progressPercentage + "%";
                        getActivity().runOnUiThread(() -> resultText.setText(progressMessage));
                    }

                    int readOffsetInMainBuffer = 0;
                    int readLengthForNewData = CHUNK_SIZE;

                    if (!isFirstChunk) {
                        System.arraycopy(previousOverlap, 0, currentChunkPlusOverlap, 0, OVERLAP_SIZE);
                        readOffsetInMainBuffer = OVERLAP_SIZE;
                    } else {
                        readLengthForNewData = CHUNK_SIZE + OVERLAP_SIZE; 
                    }
                    
                    if (currentFilePosition + readLengthForNewData > fileSize) {
                        readLengthForNewData = (int) (fileSize - currentFilePosition);
                    }

                    bytesActuallyRead = fis.read(currentChunkPlusOverlap, readOffsetInMainBuffer, readLengthForNewData);

                    if(bytesActuallyRead <= 0) break; 
                    
                    int totalDataSizeInCurrentBuffer;
                    if(isFirstChunk){
                        totalDataSizeInCurrentBuffer = bytesActuallyRead; 
                    } else {
                        totalDataSizeInCurrentBuffer = OVERLAP_SIZE + bytesActuallyRead; 
                    }
                    
                    dataToProcess = new byte[totalDataSizeInCurrentBuffer];
                    System.arraycopy(currentChunkPlusOverlap, 0, dataToProcess, 0, totalDataSizeInCurrentBuffer);

                    String result = null;
                    if (canCrackMD5) {
                        result = CryptoUtils.crackMD5(hashToCrack, dataToProcess);
                    } else if (canCrackSHA1) {
                        result = CryptoUtils.crackSHA1(hashToCrack, dataToProcess);
                    } else if (canCrackSHA256) {
                        result = CryptoUtils.crackSHA256(hashToCrack, dataToProcess);
                    }
                    
                    if (result != null) {
                        foundPlaintext = result;
                        break; 
                    }

                    if (totalDataSizeInCurrentBuffer >= OVERLAP_SIZE) {
                        System.arraycopy(dataToProcess, totalDataSizeInCurrentBuffer - OVERLAP_SIZE, previousOverlap, 0, OVERLAP_SIZE);
                    } else {
                        System.arraycopy(dataToProcess, 0, previousOverlap, 0, totalDataSizeInCurrentBuffer);
                        Arrays.fill(previousOverlap, totalDataSizeInCurrentBuffer, OVERLAP_SIZE, (byte)0);
                    }
                    
                    if(isFirstChunk){
                         currentFilePosition += (bytesActuallyRead > CHUNK_SIZE && readLengthForNewData == (CHUNK_SIZE + OVERLAP_SIZE)) ? CHUNK_SIZE : bytesActuallyRead;
                         isFirstChunk = false;
                    } else {
                        currentFilePosition += bytesActuallyRead; 
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading dump file", e);
                showError(identifiedTypesText.toString() + "读取内存转储文件时出错: " + e.getMessage());
                return;
            }
            
            long endTime = System.currentTimeMillis();
            long duration = (endTime - startTime) / 1000;

            final String finalResult = foundPlaintext;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    analyzeButton.setEnabled(true);
                    String finalMessageText = identifiedTypesText.toString();

                    if (finalResult != null) {
                        finalMessageText += "破解成功 (" + crackType + ", 耗时 " + duration + " 秒)，明文为：\n" + finalResult;
                        resultText.setText(finalMessageText);
                        
                        copyPlaintextButtonNew.setVisibility(View.VISIBLE);
                        copyPlaintextButtonNew.setOnClickListener(v -> {
                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                            android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", finalResult);
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(getContext(), "明文已复制到剪贴板", Toast.LENGTH_SHORT).show();
                        });
                        if (oldCopyPlaintextButton != null) {
                            oldCopyPlaintextButton.setVisibility(View.GONE);
                        }

                    } else {
                        finalMessageText += "破解失败或未在内存中找到 " + crackType + " 原文 (耗时 " + duration + " 秒)。";
                        resultText.setText(finalMessageText);
                        copyPlaintextButtonNew.setVisibility(View.GONE);
                        if (oldCopyPlaintextButton != null) {
                            oldCopyPlaintextButton.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });
    }

    private void showError(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if(getContext() != null) { 
                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                }
                resultText.setText(message);
                analyzeButton.setEnabled(true);
                copyPlaintextButtonNew.setVisibility(View.GONE);
                if (oldCopyPlaintextButton != null) {
                     oldCopyPlaintextButton.setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdownNow(); 
        }
    }
} 