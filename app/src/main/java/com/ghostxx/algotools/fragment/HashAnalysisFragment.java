package com.ghostxx.algotools.fragment;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.ghostxx.algotools.R;
import com.ghostxx.algotools.common.ui.InputFieldView;
import com.ghostxx.algotools.common.ui.ResultCardView;
import com.ghostxx.algotools.model.AnalysisResult;
import com.ghostxx.algotools.utils.HashCryptoUtils;
import com.ghostxx.algotools.viewmodel.HashAnalysisViewModel;

/**
 * 哈希分析界面Fragment
 * 功能：
 * 1. 提供哈希值输入
 * 2. 提供特征字符串输入（可选，用于优化搜索）
 * 3. 展示分析结果
 */
public class HashAnalysisFragment extends Fragment {
    private static final String TAG = "HashAnalysisFragment";
    
    // 界面元素
    private InputFieldView hashInputView;          // 哈希值输入框
    private InputFieldView featureInputView;       // 特征字符串输入框
    private Button analyzeButton;                 // 分析按钮
    private ResultCardView resultCardView;         // 结果卡片
    private CheckBox jniLoggingCheckbox;          // JNI日志复选框

    // ViewModel
    private HashAnalysisViewModel viewModel;
    
    // 保存找到的原文
    private String foundPlaintext = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 初始化ViewModel
        viewModel = new ViewModelProvider(this).get(HashAnalysisViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_hash_analysis, container, false);
        initializeViews(view);
        setupObservers();
        return view;
    }
        
    private void initializeViews(View view) {
        // 绑定界面元素
        hashInputView = view.findViewById(R.id.hashInputView);
        featureInputView = view.findViewById(R.id.featureInputView);
        analyzeButton = view.findViewById(R.id.analyzeButton);
        resultCardView = view.findViewById(R.id.resultCardView);
        jniLoggingCheckbox = view.findViewById(R.id.jniLoggingCheckbox);

        // 设置按钮事件
        analyzeButton.setOnClickListener(v -> startAnalysis());
        
        // 设置JNI日志开关
        setupJniLogging();
        
        // 自定义ResultCardView的复制按钮行为
        resultCardView.setOnCopyButtonClickListener(() -> {
            if (foundPlaintext != null && !foundPlaintext.isEmpty()) {
                copyToClipboard(foundPlaintext);
                showToast("已复制原文到剪贴板");
                return true; // 返回true表示已处理复制事件
            }
            return false; // 返回false表示使用默认复制行为
        });
    }
    
    /**
     * 设置ViewModel观察者
     */
    @SuppressLint("SetTextI18n")
    private void setupObservers() {
        // 观察状态消息
        viewModel.getStatusMessage().observe(getViewLifecycleOwner(), message -> {
            resultCardView.setContent(message);
        });
        
        // 观察分析结果
        viewModel.getAnalysisResult().observe(getViewLifecycleOwner(), this::handleAnalysisResult);
        
        // 观察加载状态
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            analyzeButton.setEnabled(!isLoading);
            if (isLoading) {
                resultCardView.setCopyButtonVisible(false);
            }
        });
        
        // 观察进度
        viewModel.getProgressPercent().observe(getViewLifecycleOwner(), percent -> {
            if (percent > 0) {
                String currentStatus = resultCardView.getContent();
                if (!currentStatus.contains("进度:")) {
                    resultCardView.setContent(currentStatus + "\n进度: " + percent + "%");
                } else {
                    String newStatus = currentStatus.replaceAll("进度: \\d+%", "进度: " + percent + "%");
                    resultCardView.setContent(newStatus);
                }
            }
        });
        
        // 观察找到的原文
        viewModel.getLastFoundPlaintext().observe(getViewLifecycleOwner(), plaintext -> {
            this.foundPlaintext = plaintext;
        });
    }
    
    /**
     * 处理分析结果
     */
    private void handleAnalysisResult(AnalysisResult result) {
        if (result != null) {
            resultCardView.setCopyButtonVisible(result.isSuccess());
        }
    }

    private void setupJniLogging() {
        jniLoggingCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            HashCryptoUtils.enableJniLogging(isChecked);
            String status = isChecked ? "已启用" : "已禁用";
            showToast("JNI 日志 " + status);
        });
        
        HashCryptoUtils.enableJniLogging(false);
        jniLoggingCheckbox.setChecked(false);
    }
    
    private void startAnalysis() {
        // 获取输入
        String hashToAnalyze = hashInputView.getText().trim();
        String featureString = featureInputView.getText().trim();
        
        // 调用ViewModel进行分析
        viewModel.analyzeHash(hashToAnalyze, featureString);
    }
    
    /**
     * 复制文本到剪贴板
     */
    private void copyToClipboard(String text) {
        Context context = getContext();
        if (context != null) {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("原文", text);
            clipboard.setPrimaryClip(clip);
        }
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
        // 在这里可以做一些清理工作
    }
} 