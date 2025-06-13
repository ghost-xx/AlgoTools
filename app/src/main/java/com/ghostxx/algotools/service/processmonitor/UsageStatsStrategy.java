package com.ghostxx.algotools.service.processmonitor;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;
import android.util.Log;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 使用UsageStats API检测前台应用的策略实现
 */
public class UsageStatsStrategy implements ProcessDetectionStrategy {
    private static final String TAG = "UsageStatsStrategy";
    private Context context;
    private PackageFilterManager packageFilterManager;

    @Override
    public void init(Context context) {
        this.context = context.getApplicationContext();
        this.packageFilterManager = new PackageFilterManager();
    }

    @Override
    public String getForegroundAppPackageName() {
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
                    String packageName = recentStats.getPackageName();
                    if (!packageFilterManager.isBlacklisted(packageName)) {
                        Log.d(TAG, "使用UsageStats找到前台应用: " + packageName);
                        return packageName;
                    } else {
                        Log.d(TAG, "使用UsageStats找到的应用在黑名单中: " + packageName);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "使用UsageStats获取前台应用时出错: " + e.getMessage(), e);
        }
        return "";
    }

    @Override
    public int getPriority() {
        return 1; // 最高优先级
    }

    @Override
    public boolean isAvailable() {
        return hasUsageStatsPermission();
    }

    @Override
    public String getName() {
        return "UsageStats";
    }

    /**
     * 检查是否有使用情况统计权限
     */
    public boolean hasUsageStatsPermission() {
        try {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(), context.getPackageName());
            if (mode == AppOpsManager.MODE_DEFAULT) {
                return context.checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED;
            }
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            Log.e(TAG, "检查使用情况统计权限时出错: " + e.getMessage(), e);
            return false;
        }
    }
} 