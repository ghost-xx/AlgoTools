package com.ghostxx.algotools.common.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ghostxx.algotools.R;
import com.google.android.material.card.MaterialCardView;

/**
 * 可重用的结果卡片组件
 */
public class ResultCardView extends FrameLayout {

    private TextView titleTextView;
    private TextView contentTextView;
    private ImageButton copyButton;
    
    // 自定义复制按钮点击监听器
    private OnCopyButtonClickListener onCopyButtonClickListener;
    
    // 复制按钮点击监听器接口
    public interface OnCopyButtonClickListener {
        /**
         * 当复制按钮被点击时调用
         * @return true表示已处理复制事件，false表示使用默认复制行为
         */
        boolean onCopyButtonClick();
    }
    
    public ResultCardView(@NonNull Context context) {
        super(context);
        init(context, null);
    }
    
    public ResultCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }
    
    public ResultCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }
    
    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.component_result_card, this, true);

        titleTextView = findViewById(R.id.resultTitle);
        contentTextView = findViewById(R.id.resultContent);
        copyButton = findViewById(R.id.copyButton);
        
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ResultCardView);
            
            // 设置标题
            String title = a.getString(R.styleable.ResultCardView_cardTitle);
            if (title != null) {
                titleTextView.setText(title);
            }
            
            // 设置内容
            String content = a.getString(R.styleable.ResultCardView_cardContent);
            if (content != null) {
                contentTextView.setText(content);
            }
            
            a.recycle();
        }
        
        // 设置复制按钮点击事件
        copyButton.setOnClickListener(v -> {
            // 如果设置了自定义监听器，优先使用自定义监听器
            if (onCopyButtonClickListener != null && onCopyButtonClickListener.onCopyButtonClick()) {
                // 自定义监听器已处理，不执行默认行为
                return;
            }
            
            // 默认复制行为
            String content = contentTextView.getText().toString();
            if (!content.isEmpty()) {
                copyToClipboard(context, content);
                Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 设置自定义复制按钮点击监听器
     */
    public void setOnCopyButtonClickListener(OnCopyButtonClickListener listener) {
        this.onCopyButtonClickListener = listener;
    }
    
    /**
     * 设置标题
     */
    public void setTitle(String title) {
        titleTextView.setText(title);
    }
    
    /**
     * 设置内容
     */
    public void setContent(String content) {
        contentTextView.setText(content);
    }
    
    /**
     * 获取内容
     */
    public String getContent() {
        return contentTextView.getText().toString();
    }
    
    /**
     * 设置是否显示复制按钮
     */
    public void setCopyButtonVisible(boolean visible) {
        copyButton.setVisibility(visible ? VISIBLE : GONE);
    }
    
    /**
     * 复制内容到剪贴板
     */
    private void copyToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("结果", text);
        clipboard.setPrimaryClip(clip);
    }
} 