package com.ghostxx.algotools.common.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ghostxx.algotools.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * 可重用的输入字段组件
 */
public class InputFieldView extends FrameLayout {

    private TextInputLayout inputLayout;
    private TextInputEditText inputEditText;

    public InputFieldView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public InputFieldView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public InputFieldView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.component_input_field, this, true);
        
        inputLayout = findViewById(R.id.inputLayout);
        inputEditText = findViewById(R.id.inputEditText);
        
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.InputFieldView);
            
            // 设置提示文本
            String hint = a.getString(R.styleable.InputFieldView_inputHint);
            if (hint != null) {
                inputLayout.setHint(hint);
            }
            
            // 设置输入类型
            int inputType = a.getInt(R.styleable.InputFieldView_android_inputType, InputType.TYPE_CLASS_TEXT);
            inputEditText.setInputType(inputType);
            
            // 设置最大行数
            int maxLines = a.getInt(R.styleable.InputFieldView_android_maxLines, 1);
            inputEditText.setMaxLines(maxLines);
            
            a.recycle();
        }
    }
    
    /**
     * 获取输入文本
     */
    public String getText() {
        return inputEditText.getText() != null ? inputEditText.getText().toString() : "";
    }
    
    /**
     * 设置输入文本
     */
    public void setText(String text) {
        inputEditText.setText(text);
    }
    
    /**
     * 设置错误信息
     */
    public void setError(String error) {
        inputLayout.setError(error);
    }
    
    /**
     * 清除错误信息
     */
    public void clearError() {
        inputLayout.setError(null);
    }
    
    /**
     * 获取TextInputLayout
     */
    public TextInputLayout getInputLayout() {
        return inputLayout;
    }
    
    /**
     * 获取TextInputEditText
     */
    public TextInputEditText getEditText() {
        return inputEditText;
    }
} 