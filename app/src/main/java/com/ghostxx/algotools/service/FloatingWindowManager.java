package com.ghostxx.algotools.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.ghostxx.algotools.R;

public class FloatingWindowManager {
    private final Context context;
    private final WindowManager windowManager;
    private View floatingView;
    private TextView tvProcessInfo;
    private TextView tvLog;
    private ViewGroup expandedLayout;
    private boolean isExpanded = false;
    private final FloatingWindowCallback callback;

    public interface FloatingWindowCallback {
        void onDumpButtonClicked();


        void onCloseButtonClicked();
    }

    public FloatingWindowManager(Context context, FloatingWindowCallback callback) {
        this.context = context;
        this.callback = callback;
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    @SuppressLint("InflateParams")
    public void createFloatingWindow() {
        // 加载悬浮窗布局
        floatingView = LayoutInflater.from(context).inflate(R.layout.layout_floating_dump, null);

        // 初始化视图
        tvProcessInfo = floatingView.findViewById(R.id.tv_process_info);
        tvLog = floatingView.findViewById(R.id.tv_log);
        expandedLayout = floatingView.findViewById(R.id.expanded_layout);
        ImageButton btnClose = floatingView.findViewById(R.id.btn_close);
        ImageButton btnExpand = floatingView.findViewById(R.id.btn_expand);

        // 设置按钮点击事件
        btnClose.setOnClickListener(v -> {
            if (callback != null) callback.onCloseButtonClicked();
        });

        btnExpand.setOnClickListener(v -> {
            if (callback != null) callback.onDumpButtonClicked();
        });

        // 设置悬浮窗参数
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        // 添加触摸事件处理
        setupTouchListener(params);

        // 添加悬浮窗到窗口管理器
        windowManager.addView(floatingView, params);
    }

    private void setupTouchListener(final WindowManager.LayoutParams params) {
        final float[] lastTouchX = {0};
        final float[] lastTouchY = {0};
        final int[] initialX = {0};
        final int[] initialY = {0};

        floatingView.setOnTouchListener(new View.OnTouchListener() {

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastTouchX[0] = event.getRawX();
                        lastTouchY[0] = event.getRawY();
                        initialX[0] = params.x;
                        initialY[0] = params.y;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - lastTouchX[0];
                        float dy = event.getRawY() - lastTouchY[0];
                        params.x = initialX[0] + (int) dx;
                        params.y = initialY[0] + (int) dy;
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });
    }

    public void updateProcessInfo(String info, int pid) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (tvProcessInfo != null) {
                tvProcessInfo.setText(info);
            }
        });
    }

    public void appendLog(String log) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (tvLog != null) {
                tvLog.append(log + "\n");
            }
        });
    }

    public void toggleExpand() {
        isExpanded = !isExpanded;
        expandedLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
    }

    public void ensureExpanded() {
        if (!isExpanded) {
            isExpanded = true;
            expandedLayout.setVisibility(View.VISIBLE);
        }
    }

    public void removeFloatingWindow() {
        if (floatingView != null && floatingView.isAttachedToWindow()) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }
    }
} 