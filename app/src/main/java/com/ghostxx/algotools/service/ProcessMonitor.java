package com.ghostxx.algotools.service;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import com.ghostxx.algotools.model.AppInfo;
import com.ghostxx.algotools.service.processmonitor.ProcessDetector;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 进程监控类，负责监控前台应用并获取相关信息
 * 重构后作为ProcessDetector的包装类，保持向后兼容性
 */
public class ProcessMonitor {
    private static final String TAG = "ProcessMonitor";
    private final ProcessDetector processDetector;
    private final Context context;

    // 包名黑名单，这些包名将被忽略
    private static final Set<String> PACKAGE_BLACKLIST = new HashSet<>(Arrays.asList(
            "android", // 系统级 "android" 包
            // 常见的输入法包名示例 (您可以根据需要添加更多)
            "com.android.inputmethod.latin",
            "com.google.android.inputmethod.latin", // Gboard
            "com.sohu.inputmethod.sogou", // 搜狗输入法
            "com.baidu.inputmethod_oppo", // 百度输入法 OPPO 版
            "com.baidu.inputmethod_vivo", // 百度输入法 VIVO 版
            "com.baidu.inputmethod_huawei", // 百度输入法华为版
            "com.baidu.inputmethod_xiaomi", // 百度输入法小米版
            "com.iflytek.inputmethod", // 讯飞输入法
            "com.touchtype.swiftkey", // SwiftKey
            "com.ghostxx.algotools",

            // 其他可能需要忽略的系统界面或服务
            "com.android.systemui",
            "com.miui.home"

    ));

    public ProcessMonitor(Context context) {
        this.context = context;
        this.processDetector = new ProcessDetector(context);
    }

    /**
     * 检查包名是否在黑名单中
     * @param packageName 要检查的包名
     * @return 如果在黑名单中则返回 true，否则返回 false
     */
    private static boolean isBlacklisted(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        boolean blacklisted = PACKAGE_BLACKLIST.contains(packageName);
        if (blacklisted) {
            Log.d(TAG, "包名 '" + packageName + "' 在黑名单中。");
        }
        return blacklisted;
    }

    /**
     * 获取前台应用包名
     * @return 前台应用包名，如果获取失败则返回空字符串
     */
    public String getForegroundAppPackageName() {
        return processDetector.getForegroundAppPackageName();
    }
    
    /**
     * 检查是否有使用情况统计权限
     */
    public boolean hasUsageStatsPermission() {
        return processDetector.hasUsageStatsPermission();
    }

    /**
     * 请求使用情况统计权限
     */
    public void requestUsageStatsPermission() {
        try {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.d(TAG, "已跳转到使用情况统计权限设置页面");
        } catch (Exception e) {
            Log.e(TAG, "跳转到使用情况统计权限设置页面时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 根据包名获取应用PID
     * @param packageName 包名
     * @return 应用PID，如果获取失败则返回-1
     */
    public int getPidByPackageName(String packageName) {
        return processDetector.getPidByPackageName(packageName);
    }

    /**
     * 获取应用名称
     * @param packageName 包名
     * @return 应用名称，如果获取失败则返回包名
     */
    public String getAppName(String packageName) {
        return processDetector.getAppName(packageName);
    }

    /**
     * 获取前台应用信息
     * @return 前台应用信息，如果获取失败则返回null
     */
    public AppInfo getForegroundApp() {
        return processDetector.getForegroundApp();
    }

    /**
     * 检查是否有root权限
     * @return true如果有root权限，false如果没有
     */
    public boolean checkRootAccess() {
        try {
            java.lang.Process process = Runtime.getRuntime().exec("su -c id");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            boolean hasRoot = line != null && line.contains("uid=0");
            reader.close();
            process.waitFor();
            return hasRoot;
        } catch (Exception e) {
            Log.e(TAG, "Root检查失败: " + e.getMessage());
            return false;
        }
    }
}