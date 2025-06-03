package com.ghostxx.algotools.service;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;

import com.ghostxx.algotools.model.AppInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 进程监控类，负责监控前台应用并获取相关信息
 */
public class ProcessMonitor {
    private static final String TAG = "ProcessMonitor";
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
    }

    /**
     * 检查包名是否在黑名单中
     * @param packageName 要检查的包名
     * @return 如果在黑名单中则返回 true，否则返回 false
     */
    private boolean isBlacklisted(String packageName) {
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
     * 获取前台应用包名，尝试多种方法
     * @return 前台应用包名，如果获取失败则返回空字符串
     */
    public String getForegroundAppPackageName() {
        String pkgName;

        // 方法0: 使用UsageStatsManager (最可靠但需要特殊权限)
        pkgName = getForegroundAppUsingUsageStats();
        if (!pkgName.isEmpty()) {
            if (!isBlacklisted(pkgName)) {
                Log.d(TAG, "方法0 (UsageStats) 最终使用包名: " + pkgName);
                return pkgName;
            } else {
                // 如果在黑名单中，则记录并继续尝试其他方法
                Log.d(TAG, "方法0 (UsageStats) 获取到包名: " + pkgName + " (已列入黑名单，尝试其他方法)");
            }
        }

        // 如果UsageStats未获取到有效(非黑名单)包名，则尝试Shell命令
        // 重置pkgName，用于Shell命令的尝试结果
        try {
            // 方法一: 使用 dumpsys window windows
            String tempCandidate = null; // 用于存储当前方法找到的候选包名
            java.lang.Process process = Runtime.getRuntime().exec("su -c dumpsys window windows | grep -E 'mCurrentFocus|mFocusedApp'");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.contains("mCurrentFocus") || line.contains("mFocusedApp")) {
                    Log.d(TAG, "方法1 (dumpsys window) 窗口焦点信息: " + line);
                    String extractedPart = ""; // 当前行提取到的非黑名单包名
                    if (line.contains("/")) {
                        String[] parts = line.split("/")[0].split(" ");
                        for (String part : parts) {
                            if (part.contains(".")) { // 基础检查，判断是否像包名
                                if (!isBlacklisted(part)) {
                                    extractedPart = part;
                                    Log.d(TAG, "方法1 (dumpsys window) 提取到候选包名: " + extractedPart);
                                    break; // 从当前行的parts中找到一个非黑名单的，跳出内层循环
                                } else {
                                    Log.d(TAG, "方法1 (dumpsys window) 提取到包名: " + part + " (已列入黑名单，继续解析当前行)");
                                }
                            }
                        }
                    }
                    if (!extractedPart.isEmpty()) {
                        tempCandidate = extractedPart;
                        break; // 从 mCurrentFocus/mFocusedApp 相关行中找到一个非黑名单的，跳出外层while循环
                    }
                }
            }
            reader.close();
            process.waitFor();

            if (tempCandidate != null) {
                Log.d(TAG, "方法1 (dumpsys window) 最终使用包名: " + tempCandidate);
                return tempCandidate;
            }

            // 方法二: 使用 dumpsys activity recents (仅当方法一失败或未找到非黑名单包时)
            Log.d(TAG, "方法1 未获取到有效包名，尝试方法二 (dumpsys recents)");
            // 重置候选包名
            process = Runtime.getRuntime().exec("su -c dumpsys activity recents | grep 'Recent #0' -A2");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            while ((line = reader.readLine()) != null) {
                Log.d(TAG, "方法2 (dumpsys recents) 近期任务信息: " + line);
                if (line.contains(" packageName=")) {
                    String[] parts = line.trim().split("packageName=");
                    if (parts.length > 1) {
                        String extractedPkg = parts[1].split(" ")[0];
                        if (!isBlacklisted(extractedPkg)) {
                            tempCandidate = extractedPkg;
                            Log.d(TAG, "方法2 (dumpsys recents) 提取到候选包名: " + tempCandidate);
                            break; // 找到非黑名单包，跳出循环
                        } else {
                            Log.d(TAG, "方法2 (dumpsys recents) 提取到包名: " + extractedPkg + " (已列入黑名单，继续解析当前行)");
                        }
                    }
                }
            }
            reader.close();
            process.waitFor();

            if (tempCandidate != null && !tempCandidate.isEmpty()) {
                Log.d(TAG, "方法2 (dumpsys recents) 最终使用包名: " + tempCandidate);
                return tempCandidate;
            }

            // 方法三: 使用 dumpsys activity activities (仅当方法二失败或未找到非黑名单包时)
            Log.d(TAG, "方法2 未获取到有效包名，尝试方法三 (dumpsys activities)");
            tempCandidate = null; // 重置候选包名
            process = Runtime.getRuntime().exec("su -c dumpsys activity activities | grep mResumedActivity");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            line = reader.readLine(); // 此命令通常只关心第一行输出
            
            if (line != null) {
                Log.d(TAG, "方法3 (dumpsys activities) 已恢复活动信息: " + line);
                if (line.contains(" ")) {
                    String[] segments = line.split("\\s+");
                    for (String segment : segments) {
                        if (segment.contains("/") && segment.contains(".")) {
                            String extractedPkg = segment.split("/")[0];
                            if (!isBlacklisted(extractedPkg)) {
                                tempCandidate = extractedPkg;
                                Log.d(TAG, "方法3 (dumpsys activities) 提取到候选包名: " + tempCandidate);
                                break; // 从segments中找到一个非黑名单的，跳出循环
                            } else {
                                Log.d(TAG, "方法3 (dumpsys activities) 提取到包名: " + extractedPkg + " (已列入黑名单，继续解析当前行)");
                            }
                        }
                    }
                }
            }
            reader.close();
            process.waitFor();
            
            if (tempCandidate != null && !tempCandidate.isEmpty()) {
                Log.d(TAG, "方法3 (dumpsys activities) 最终使用包名: " + tempCandidate);
                return tempCandidate;
            }
            
            Log.d(TAG, "所有方法均未获取到有效(非黑名单)包名。");
            return ""; // 所有方法均失败或只找到黑名单中的包，返回空字符串

        } catch (Exception e) {
            Log.e(TAG, "获取前台应用Shell命令执行时出错: " + e.getMessage(), e);
            return ""; // Shell命令执行出错，返回空字符串
        }
    }

    /**
     * 使用UsageStats API获取前台应用包名
     * @return 前台应用包名，如果获取失败则返回空字符串
     */
    private String getForegroundAppUsingUsageStats() {
        try {
            UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            long currentTime = System.currentTimeMillis();
            // 获取最近1分钟的应用使用情况
            long startTime = currentTime - 60 * 1000;
            
            // 检查是否有权限访问使用情况统计
            if (!hasUsageStatsPermission()) {
                Log.d(TAG, "没有使用情况统计权限，无法使用UsageStats API");
                return "";
            }
            
            List<UsageStats> queryUsageStats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY, startTime, currentTime);
            
            if (queryUsageStats == null || queryUsageStats.isEmpty()) {
                Log.d(TAG, "未获取到使用情况统计数据");
                return "";
            }
            
            // 找出最近使用的应用
            SortedMap<Long, UsageStats> sortedMap = new TreeMap<>();
            for (UsageStats usageStats : queryUsageStats) {
                sortedMap.put(usageStats.getLastTimeUsed(), usageStats);
            }
            
            if (!sortedMap.isEmpty()) {
                UsageStats recentStats = sortedMap.get(sortedMap.lastKey());
                if (recentStats != null) {
                    return recentStats.getPackageName();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "使用UsageStats获取前台应用时出错: " + e.getMessage(), e);
        }
        return "";
    }
    
    /**
     * 检查是否有使用情况统计权限
     * @return true如果有权限，false如果没有
     */
    public boolean hasUsageStatsPermission() {
        try {
            // 使用AppOpsManager更准确地检测权限
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(), context.getPackageName());
            } else {
                mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(), context.getPackageName());
            }
            
            if (mode == AppOpsManager.MODE_ALLOWED) {
                // 再次确认权限是否真的有效
                UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
                long currentTime = System.currentTimeMillis();
                List<UsageStats> stats = usageStatsManager.queryUsageStats(
                        UsageStatsManager.INTERVAL_DAILY, currentTime - 3600 * 1000, currentTime);
                return stats != null && !stats.isEmpty();
            }
            
            return false;
        } catch (Exception e) {
            Log.e(TAG, "检查使用情况统计权限时出错: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 打开使用情况统计权限设置页面
     */
    public void requestUsageStatsPermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "打开使用情况统计权限设置页面失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据包名获取进程ID
     * @param packageName 应用包名
     * @return 进程ID，如果未找到则返回-1
     */
    public int getPidByPackageName(String packageName) {
        try {
            // 添加日志以便调试
            Log.d(TAG, "尝试获取包名 " + packageName + " 的PID");
            
            // 尝试两种方式获取PID
            java.lang.Process process = Runtime.getRuntime().exec("su -c ps -A | grep " + packageName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            // 记录所有输出行以便调试
            StringBuilder allLines = new StringBuilder();
            while (line != null) {
                allLines.append(line).append("\n");
                // 尝试解析第一行
                if (line.contains(packageName)) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 2) {
                        try {
                            int pid = Integer.parseInt(parts[1]);
                            Log.d(TAG, "成功从ps -A输出解析到PID: " + pid);
                            reader.close();
                            process.waitFor();
                            return pid;
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "解析PID时出错: " + parts[1], e);
                        }
                    }
                }
                line = reader.readLine();
            }
            
            Log.d(TAG, "ps -A输出: \n" + allLines.toString());
            reader.close();
            process.waitFor();
            
            // 如果第一种方式失败，尝试第二种
            process = Runtime.getRuntime().exec("su -c pidof " + packageName);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            line = reader.readLine();
            
            if (line != null && !line.isEmpty()) {
                try {
                    int pid = Integer.parseInt(line.trim());
                    Log.d(TAG, "通过pidof命令获取到PID: " + pid);
                    reader.close();
                    process.waitFor();
                    return pid;
                } catch (NumberFormatException e) {
                    Log.e(TAG, "解析pidof输出时出错: " + line, e);
                }
            }
            
            reader.close();
            process.waitFor();
        } catch (Exception e) {
            Log.e(TAG, "获取PID时出错: " + e.getMessage(), e);
        }
        return -1;
    }


    /**
     * 获取应用的真实名称
     * @param packageName 应用包名
     * @return 应用名称，如果获取失败则返回包名
     */
    public String getAppName(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return "无应用";
        }
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return (String) pm.getApplicationLabel(ai);
        } catch (Exception e) {
            // 如果无法获取应用名称，直接返回包名
            return packageName;
        }
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

    public AppInfo getForegroundApp() {
        String packageName = getForegroundAppPackageName();
        int pid = getPidByPackageName(packageName);
        String appName = getAppName(packageName);
        return new AppInfo(packageName, pid, appName);
    }

}