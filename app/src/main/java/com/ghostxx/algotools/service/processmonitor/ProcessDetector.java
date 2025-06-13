package com.ghostxx.algotools.service.processmonitor;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.ghostxx.algotools.model.AppInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 进程探测器
 * 管理和协调多种进程检测策略，提供统一的接口获取前台应用信息
 */
public class ProcessDetector {
    private static final String TAG = "ProcessDetector";
    private final Context context;
    private final List<ProcessDetectionStrategy> strategies = new ArrayList<>();
    private final PackageFilterManager packageFilterManager = new PackageFilterManager();

    public ProcessDetector(Context context) {
        this.context = context.getApplicationContext();
        initStrategies();
    }

    /**
     * 初始化所有进程检测策略
     */
    private void initStrategies() {
        // 添加所有可用的策略，并按照优先级排序
        addStrategy(new UsageStatsStrategy());
        addStrategy(new ShellCommandStrategy());
        
        // 根据优先级排序策略
        Collections.sort(strategies, (s1, s2) -> Integer.compare(s1.getPriority(), s2.getPriority()));
        
        Log.d(TAG, "已初始化 " + strategies.size() + " 个进程检测策略");
    }

    /**
     * 添加并初始化策略
     */
    private void addStrategy(ProcessDetectionStrategy strategy) {
        strategy.init(context);
        strategies.add(strategy);
        Log.d(TAG, "已添加策略: " + strategy.getName() + ", 优先级: " + strategy.getPriority() + 
              ", 可用: " + strategy.isAvailable());
    }

    /**
     * 获取前台应用信息
     * @return 前台应用信息，如果获取失败则返回null
     */
    public AppInfo getForegroundApp() {
        String packageName = getForegroundAppPackageName();
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }
        
        int pid = getPidByPackageName(packageName);
        String appName = getAppName(packageName);
        
        return new AppInfo(packageName, pid, appName);
    }

    /**
     * 获取前台应用包名
     * @return 前台应用包名，如果获取失败则返回空字符串
     */
    public String getForegroundAppPackageName() {
        // 按优先级依次尝试各种策略
        for (ProcessDetectionStrategy strategy : strategies) {
            if (strategy.isAvailable()) {
                String packageName = strategy.getForegroundAppPackageName();
                if (packageName != null && !packageName.isEmpty() && 
                    !packageFilterManager.isBlacklisted(packageName)) {
                    Log.d(TAG, "使用 " + strategy.getName() + " 策略成功获取前台应用: " + packageName);
                    return packageName;
                }
            }
        }
        
        Log.d(TAG, "所有策略均未获取到有效前台应用");
        return "";
    }

    /**
     * 根据包名获取应用PID
     * @param packageName 包名
     * @return 应用PID，如果获取失败则返回-1
     */
    public int getPidByPackageName(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return -1;
        }
        
        try {
            // 尝试使用Shell命令获取PID
            java.lang.Process process = Runtime.getRuntime().exec("su -c ps -A | grep " + packageName);
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            String line;
            int pid = -1;
            
            while ((line = reader.readLine()) != null) {
                Log.d(TAG, "进程信息: " + line);
                if (line.contains(packageName)) {
                    // 解析ps命令输出的PID (第二列)
                    String[] columns = line.trim().split("\\s+");
                    if (columns.length >= 2) {
                        try {
                            pid = Integer.parseInt(columns[1]);
                            break;
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "解析PID时出错: " + e.getMessage(), e);
                        }
                    }
                }
            }
            reader.close();
            process.waitFor();
            
            if (pid > 0) {
                Log.d(TAG, "获取到 " + packageName + " 的PID: " + pid);
                return pid;
            }
            
            // 如果ps命令失败，尝试使用pidof命令
            process = Runtime.getRuntime().exec("su -c pidof " + packageName);
            reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            line = reader.readLine();
            reader.close();
            process.waitFor();
            
            if (line != null && !line.isEmpty()) {
                try {
                    pid = Integer.parseInt(line.trim());
                    Log.d(TAG, "使用pidof获取到 " + packageName + " 的PID: " + pid);
                    return pid;
                } catch (NumberFormatException e) {
                    Log.e(TAG, "解析pidof输出时出错: " + e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "获取PID时出错: " + e.getMessage(), e);
        }
        
        Log.d(TAG, "未能获取 " + packageName + " 的PID");
        return -1;
    }

    /**
     * 获取应用名称
     * @param packageName 包名
     * @return 应用名称，如果获取失败则返回包名
     */
    public String getAppName(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return "";
        }
        
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return (String) pm.getApplicationLabel(ai);
        } catch (Exception e) {
            Log.e(TAG, "获取应用名称时出错: " + e.getMessage(), e);
            return packageName; // 返回包名作为后备
        }
    }

    /**
     * 检查是否有使用情况统计权限
     */
    public boolean hasUsageStatsPermission() {
        for (ProcessDetectionStrategy strategy : strategies) {
            if (strategy instanceof UsageStatsStrategy) {
                return strategy.isAvailable();
            }
        }
        return false;
    }
} 