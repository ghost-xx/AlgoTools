package com.ghostxx.algotools.service.processmonitor;

import android.content.Context;

/**
 * 进程检测策略接口
 * 定义不同进程检测方法的共同行为
 */
public interface ProcessDetectionStrategy {
    /**
     * 初始化策略
     * @param context 应用上下文
     */
    void init(Context context);
    
    /**
     * 获取前台应用包名
     * @return 前台应用包名，如果获取失败则返回空字符串
     */
    String getForegroundAppPackageName();
    
    /**
     * 获取策略的优先级，数值越小优先级越高
     * @return 优先级值
     */
    int getPriority();
    
    /**
     * 检查该策略是否可用
     * @return 如果可用返回true，否则返回false
     */
    boolean isAvailable();
    
    /**
     * 获取策略名称，用于日志
     * @return 策略名称
     */
    String getName();
} 